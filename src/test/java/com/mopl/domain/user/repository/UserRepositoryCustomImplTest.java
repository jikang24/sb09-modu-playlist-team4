package com.mopl.domain.user.repository;

import com.mopl.domain.user.domain.User;
import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.UserSearchRequest;
import com.mopl.domain.user.dto.UserSortBy;
import com.mopl.global.config.JpaConfig;
import com.mopl.global.config.QueryDslConfig;
import com.mopl.global.dto.SortDirection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.sql.init.mode=never")
@Import({QueryDslConfig.class, JpaConfig.class, UserRepositoryCustomImpl.class})
@DisplayName("UserRepositoryCustomImpl 테스트")
class UserRepositoryCustomImplTest {

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    private User saveUser(String name, String email, Role role, boolean locked, Instant createdAt) {
        User user = User.builder()
                .name(name)
                .email(email)
                .password("encoded")
                .role(role)
                .locked(locked)
                .build();
        User saved = userRepository.save(user);
        em.createQuery("UPDATE User u SET u.createdAt = :createdAt WHERE u.id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", saved.getId())
                .executeUpdate();
        em.flush();
        em.clear();
        return userRepository.findById(saved.getId()).orElseThrow();
    }

    private UserSearchRequest request(String nameLike, String emailLike, Role roleEqual, Boolean isLocked,
                                       String cursor, UUID idAfter, int limit,
                                       SortDirection direction, UserSortBy sortBy) {
        return new UserSearchRequest(nameLike, emailLike, roleEqual, isLocked, cursor, idAfter, limit, direction, sortBy);
    }

    @Nested
    @DisplayName("findAllWithCursor: 필터링")
    class Filtering {

        @BeforeEach
        void setUp() {
            saveUser("alice", "alice@mopl.io", Role.USER, false, Instant.now());
            saveUser("bob", "bob@mopl.io", Role.ADMIN, false, Instant.now());
            saveUser("charlie", "charlie@mopl.io", Role.USER, true, Instant.now());
        }

        @Test
        @DisplayName("성공: 이름에 포함된 문자열로 필터링한다")
        void filterByNameLike() {
            UserSearchRequest req = request("ali", null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).extracting(User::getName).containsExactly("alice");
        }

        @Test
        @DisplayName("성공: 이메일에 포함된 문자열로 필터링한다")
        void filterByEmailLike() {
            UserSearchRequest req = request(null, "bob", null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).extracting(User::getName).containsExactly("bob");
        }

        @Test
        @DisplayName("성공: role로 필터링한다")
        void filterByRole() {
            UserSearchRequest req = request(null, null, Role.ADMIN, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).extracting(User::getName).containsExactly("bob");
        }

        @Test
        @DisplayName("성공: locked 상태로 필터링한다")
        void filterByLocked() {
            UserSearchRequest req = request(null, null, null, true, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).extracting(User::getName).containsExactly("charlie");
        }

        @Test
        @DisplayName("성공: 필터가 없으면 모든 사용자를 반환한다")
        void noFilter_returnsAll() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).extracting(User::getName).containsExactly("alice", "bob", "charlie");
        }

        @Test
        @DisplayName("실패: 조건에 맞는 사용자가 없으면 빈 목록을 반환한다")
        void noMatch_returnsEmpty() {
            UserSearchRequest req = request("nonexistent", null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findAllWithCursor: 정렬 및 커서 페이징")
    class SortingAndCursor {

        private User u1;
        private User u2;
        private User u3;

        @BeforeEach
        void setUp() {
            Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
            u1 = saveUser("alice", "alice@mopl.io", Role.USER, false, base);
            u2 = saveUser("bob", "bob@mopl.io", Role.ADMIN, true, base.plusSeconds(60));
            u3 = saveUser("charlie", "charlie@mopl.io", Role.USER, false, base.plusSeconds(120));
        }

        @Test
        @DisplayName("성공: 이름 오름차순으로 정렬된다")
        void sortByNameAscending() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).extracting(User::getName).containsExactly("alice", "bob", "charlie");
        }

        @Test
        @DisplayName("성공: 이름 내림차순으로 정렬된다")
        void sortByNameDescending() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.DESCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).extracting(User::getName).containsExactly("charlie", "bob", "alice");
        }

        @Test
        @DisplayName("성공: limit보다 많은 데이터가 있으면 limit+1개를 반환한다 (hasNext 판단용)")
        void fetchesOneExtraForHasNext() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    2, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("성공: 커서 이후의 데이터만 조회한다 (이름 기준)")
        void cursorPagination_byName() {
            UserSearchRequest req = request(null, null, null, null, "bob", u2.getId(),
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).extracting(User::getName).containsExactly("charlie");
        }

        @Test
        @DisplayName("성공: 이메일 기준으로 정렬 및 커서 페이징한다")
        void sortAndCursor_byEmail() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.EMAIL);

            List<User> result = userRepository.findAllWithCursor(req);
            assertThat(result).extracting(User::getEmail)
                    .containsExactly("alice@mopl.io", "bob@mopl.io", "charlie@mopl.io");

            UserSearchRequest cursorReq = request(null, null, null, null, "alice@mopl.io", u1.getId(),
                    10, SortDirection.ASCENDING, UserSortBy.EMAIL);
            List<User> next = userRepository.findAllWithCursor(cursorReq);
            assertThat(next).extracting(User::getEmail)
                    .containsExactly("bob@mopl.io", "charlie@mopl.io");
        }

        @Test
        @DisplayName("성공: 생성일 기준으로 정렬 및 커서 페이징한다")
        void sortAndCursor_byCreatedAt() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.CREATEDAT);

            List<User> result = userRepository.findAllWithCursor(req);
            assertThat(result).extracting(User::getName).containsExactly("alice", "bob", "charlie");
        }

        @Test
        @DisplayName("성공: 잠금 상태 기준으로 정렬한다")
        void sortByLocked() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.ISLOCKED);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("성공: 권한 기준으로 정렬 및 커서 페이징한다")
        void sortAndCursor_byRole() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.DESCENDING, UserSortBy.ROLE);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).hasSize(3);
        }

        @ParameterizedTest(name = "sortBy={0}")
        @EnumSource(UserSortBy.class)
        @DisplayName("성공: 모든 정렬 기준에서 예외 없이 조회된다")
        void allSortByValues_work(UserSortBy sortBy) {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.ASCENDING, sortBy);

            List<User> result = userRepository.findAllWithCursor(req);

            assertThat(result).hasSize(3);
        }
    }

    @Nested
    @DisplayName("countAll: 조건별 카운트")
    class CountAll {

        @BeforeEach
        void setUp() {
            saveUser("alice", "alice@mopl.io", Role.USER, false, Instant.now());
            saveUser("bob", "bob@mopl.io", Role.ADMIN, false, Instant.now());
            saveUser("charlie", "charlie@mopl.io", Role.USER, true, Instant.now());
        }

        @Test
        @DisplayName("성공: 필터 없이 전체 개수를 센다")
        void countAll_noFilter() {
            UserSearchRequest req = request(null, null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            long count = userRepository.countAll(req);

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("성공: role 필터를 적용해 개수를 센다")
        void countAll_withRoleFilter() {
            UserSearchRequest req = request(null, null, Role.USER, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            long count = userRepository.countAll(req);

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("실패: 조건에 맞는 사용자가 없으면 0을 반환한다")
        void countAll_noMatch() {
            UserSearchRequest req = request("nonexistent", null, null, null, null, null,
                    10, SortDirection.ASCENDING, UserSortBy.NAME);

            long count = userRepository.countAll(req);

            assertThat(count).isZero();
        }
    }
}
