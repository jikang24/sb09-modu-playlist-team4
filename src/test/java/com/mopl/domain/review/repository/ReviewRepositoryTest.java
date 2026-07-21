package com.mopl.domain.review.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mopl.domain.review.domain.Review;
import com.mopl.domain.review.dto.ReviewSearchRequest;
import com.mopl.domain.review.dto.ReviewSortBy;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * @DataJpaTest 에는 @Transactional 이 포함되어 있어
 * @BeforeEach 와 각 테스트 메서드가 동일한 트랜잭션 안에서 실행됩니다.
 * @Nested 를 사용하면 @Transactional 이 중첩 클래스에 상속되지 않아
 * LazyInitializationException 이 발생하므로 메서드를 외부 클래스에 직접 선언합니다.
 *
 * NotificationRepositoryCustomImpl(QueryDSL 기반 fragment)이 컴포넌트 스캔에 걸려
 * JPAQueryFactory 빈을 요구하므로, Review와 무관하지만 테스트용으로 하나 등록해둔다.
 */
@DataJpaTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:reviewtest;MODE=PostgreSQL",
    "spring.sql.init.mode=never"
})
@Import(ReviewRepositoryTest.Config.class)
@DisplayName("ReviewRepository 통합 테스트")
class ReviewRepositoryTest {

  @TestConfiguration
  static class Config {
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
      return new JPAQueryFactory(em);
    }
  }

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private TestEntityManager testEntityManager;

  private Instant nextCreatedAt = Instant.parse("2026-01-01T00:00:00Z");


  private Review persistReview(UUID contentId, UUID userId, BigDecimal rating) {
    Review review = Review.create(contentId, userId, rating, "좋아요", "작성자", "https://image.jpg");
    Review saved = testEntityManager.persistAndFlush(review);

    Instant createdAt = nextCreatedAt;
    nextCreatedAt = nextCreatedAt.plusSeconds(1);  // 호출할 때마다 무조건 1초씩 증가

    testEntityManager.getEntityManager()
            .createQuery("UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", saved.getId())
            .executeUpdate();
    testEntityManager.flush();
    testEntityManager.clear();

    return testEntityManager.find(Review.class, saved.getId());
  }

  @Test
  @DisplayName("existsByContentIdAndUserId - 이미 리뷰가 있으면 true, 없으면 false")
  void existsByContentIdAndUserId() {
    UUID contentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    persistReview(contentId, userId, BigDecimal.valueOf(4));

    assertThat(reviewRepository.existsByContentIdAndUserId(contentId, userId)).isTrue();
    assertThat(reviewRepository.existsByContentIdAndUserId(contentId, UUID.randomUUID())).isFalse();
    assertThat(reviewRepository.existsByContentIdAndUserId(UUID.randomUUID(), userId)).isFalse();
  }

  @Test
  @DisplayName("updateAuthorSnapshotByUserId - 해당 유저가 쓴 모든 리뷰의 작성자 정보가 일괄 갱신된다")
  void updateAuthorSnapshotByUserId() {
    UUID userId = UUID.randomUUID();
    Review review1 = persistReview(UUID.randomUUID(), userId, BigDecimal.valueOf(4));
    Review review2 = persistReview(UUID.randomUUID(), userId, BigDecimal.valueOf(3));
    Review otherUsersReview = persistReview(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(5));

    reviewRepository.updateAuthorSnapshotByUserId(userId, "새이름", "https://new.jpg");
    testEntityManager.flush();
    testEntityManager.clear();

    Review updated1 = reviewRepository.findById(review1.getId()).orElseThrow();
    Review updated2 = reviewRepository.findById(review2.getId()).orElseThrow();
    Review untouched = reviewRepository.findById(otherUsersReview.getId()).orElseThrow();

    assertThat(updated1.getAuthorName()).isEqualTo("새이름");
    assertThat(updated1.getAuthorProfileImageUrl()).isEqualTo("https://new.jpg");
    assertThat(updated2.getAuthorName()).isEqualTo("새이름");
    assertThat(untouched.getAuthorName()).isEqualTo("작성자");
  }

  @Test
  @DisplayName("deleteByContentId - 해당 콘텐츠의 리뷰만 일괄 삭제되고 다른 콘텐츠의 리뷰는 남는다")
  void deleteByContentId_removesOnlyMatchingContentReviews() {
    UUID targetContentId = UUID.randomUUID();
    Review review1 = persistReview(targetContentId, UUID.randomUUID(), BigDecimal.valueOf(4));
    Review review2 = persistReview(targetContentId, UUID.randomUUID(), BigDecimal.valueOf(3));
    Review otherContentReview = persistReview(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(5));

    reviewRepository.deleteByContentId(targetContentId);
    testEntityManager.flush();
    testEntityManager.clear();

    assertThat(reviewRepository.findById(review1.getId())).isEmpty();
    assertThat(reviewRepository.findById(review2.getId())).isEmpty();
    assertThat(reviewRepository.findById(otherContentReview.getId())).isPresent();
  }

  @Test
  @DisplayName("findAllByCondition - contentId로 필터링한다")
  void findAll_filtersByContentId() {
    UUID targetContentId = UUID.randomUUID();
    persistReview(targetContentId, UUID.randomUUID(), BigDecimal.valueOf(4));
    persistReview(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(3));

    ReviewSearchRequest request = new ReviewSearchRequest(
        targetContentId, null, null, 10, ReviewSortBy.CREATED_AT, SortDirection.DESCENDING);

    List<Review> result = reviewRepository.findAllByCondition(request);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContentId()).isEqualTo(targetContentId);
  }

  @Test
  @DisplayName("findAllByCondition - createdAt 커서보다 이전 데이터만 조회한다 (내림차순)")
  void findAll_cursorByCreatedAt_descending() throws InterruptedException {
    UUID contentId = UUID.randomUUID();
    Review first = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(4));
    Thread.sleep(5);
    Review second = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(3));

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, second.getCreatedAt().toString(), second.getId(), 10,
        ReviewSortBy.CREATED_AT, SortDirection.DESCENDING);

    List<Review> result = reviewRepository.findAllByCondition(request);

    assertThat(result).extracting(Review::getId).containsExactly(first.getId());
  }

  @Test
  @DisplayName("findAllByCondition - createdAt 커서보다 이후 데이터만 조회한다 (오름차순)")
  void findAll_cursorByCreatedAt_ascending() throws InterruptedException {
    UUID contentId = UUID.randomUUID();
    Review first = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(4));
    Thread.sleep(5);
    Review second = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(3));

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, first.getCreatedAt().toString(), first.getId(), 10,
        ReviewSortBy.CREATED_AT, SortDirection.ASCENDING);

    List<Review> result = reviewRepository.findAllByCondition(request);

    assertThat(result).extracting(Review::getId).containsExactly(second.getId());
  }

  @Test
  @DisplayName("findAllByCondition - rating 커서 기준으로 필터링한다 (오름차순)")
  void findAll_cursorByRating_ascending() {
    UUID contentId = UUID.randomUUID();
    Review low = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(2));
    Review high = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(4));

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, low.getRating().toString(), low.getId(), 10,
        ReviewSortBy.RATING, SortDirection.ASCENDING);

    List<Review> result = reviewRepository.findAllByCondition(request);

    assertThat(result).extracting(Review::getId).containsExactly(high.getId());
  }

  @Test
  @DisplayName("findAllByCondition - rating 커서보다 낮은 데이터만 조회한다 (내림차순)")
  void findAll_cursorByRating_descending() {
    UUID contentId = UUID.randomUUID();
    Review low = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(2));
    Review high = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(4));

    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, high.getRating().toString(), high.getId(), 10,
        ReviewSortBy.RATING, SortDirection.DESCENDING);

    List<Review> result = reviewRepository.findAllByCondition(request);

    assertThat(result).extracting(Review::getId).containsExactly(low.getId());
  }

  @Test
  @DisplayName("findAllByCondition - createdAt 정렬인데 커서 형식이 잘못되면 예외 발생")
  void findAll_invalidCreatedAtCursor() {
    ReviewSearchRequest request = new ReviewSearchRequest(
        UUID.randomUUID(), "not-an-instant", UUID.randomUUID(), 10,
        ReviewSortBy.CREATED_AT, SortDirection.DESCENDING);

    assertThatThrownBy(() -> reviewRepository.findAllByCondition(request))
        .isInstanceOf(MoplException.class)
        .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT));
  }

  @Test
  @DisplayName("findAllByCondition - rating 정렬인데 커서 형식이 잘못되면 예외 발생")
  void findAll_invalidRatingCursor() {
    ReviewSearchRequest request = new ReviewSearchRequest(
        UUID.randomUUID(), "not-a-number", UUID.randomUUID(), 10,
        ReviewSortBy.RATING, SortDirection.DESCENDING);

    assertThatThrownBy(() -> reviewRepository.findAllByCondition(request))
        .isInstanceOf(MoplException.class)
        .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT));
  }

  @Test
  @DisplayName("countByCondition - 커서와 무관하게 콘텐츠 전체 리뷰 개수를 센다")
  void countByCondition_ignoresCursor() {
    UUID contentId = UUID.randomUUID();
    Review first = persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(4));
    persistReview(contentId, UUID.randomUUID(), BigDecimal.valueOf(3));
    persistReview(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.valueOf(5)); // 다른 콘텐츠

    // 커서가 걸려있어도(마치 두 번째 페이지를 요청하듯) totalCount는 그대로 2여야 함
    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, first.getCreatedAt().toString(), first.getId(), 10,
        ReviewSortBy.CREATED_AT, SortDirection.DESCENDING);

    assertThat(reviewRepository.countByCondition(request)).isEqualTo(2L);
  }
}
