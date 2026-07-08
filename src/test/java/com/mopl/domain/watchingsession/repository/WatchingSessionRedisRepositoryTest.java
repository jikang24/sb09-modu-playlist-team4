package com.mopl.domain.watchingsession.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;

import com.mopl.domain.watchingsession.domain.WatchingSession;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionRedisRepository 테스트")
class WatchingSessionRedisRepositoryTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private HashOperations<String, Object, Object> hashOperations;

  @Mock
  private ZSetOperations<String, String> zSetOperations;

  @Mock
  private ValueOperations<String, String> valueOperations;

  private WatchingSessionRedisRepository repository;

  @BeforeEach
  void setUp() {
    lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    repository = new WatchingSessionRedisRepository(redisTemplate);
  }

  private String sessionKey(UUID id) {
    return "watching:session:" + id;
  }

  private String watcherKey(UUID watcherId) {
    return "watching:watcher:" + watcherId;
  }

  private String contentKey(UUID contentId) {
    return "watching:description:" + contentId + ":sessions";
  }

  private Map<Object, Object> sessionFields(UUID watcherId, UUID contentId, Instant createdAt) {
    return Map.of(
        "watcherId", watcherId.toString(),
        "contentId", contentId.toString(),
        "createdAt", createdAt.toString());
  }

  @Nested
  @DisplayName("시청 입장 - enter()")
  class Enter {

    @Test
    @DisplayName("기존에 보고 있던 세션이 없으면 바로 새 세션을 만든다")
    void success_noExistingSession() {
      UUID watcherId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      given(valueOperations.get(watcherKey(watcherId))).willReturn(null);

      WatchingSession session = repository.enter(watcherId, contentId);

      assertThat(session.watcherId()).isEqualTo(watcherId);
      assertThat(session.contentId()).isEqualTo(contentId);
      then(hashOperations).should().putAll(eq(sessionKey(session.id())), anyMap());
      then(zSetOperations).should().add(
          eq(contentKey(contentId)), eq(session.id().toString()),
          eq((double) session.createdAt().toEpochMilli()));
      then(valueOperations).should().set(watcherKey(watcherId), session.id().toString());
    }

    @Test
    @DisplayName("이미 보고 있던 세션이 있으면 그 세션을 먼저 정리하고 새로 입장한다")
    void success_leavesExistingSessionFirst() {
      UUID watcherId = UUID.randomUUID();
      UUID oldContentId = UUID.randomUUID();
      UUID newContentId = UUID.randomUUID();
      UUID oldSessionId = UUID.randomUUID();

      given(valueOperations.get(watcherKey(watcherId))).willReturn(oldSessionId.toString());
      given(hashOperations.get(sessionKey(oldSessionId), "contentId")).willReturn(oldContentId.toString());

      WatchingSession session = repository.enter(watcherId, newContentId);

      then(zSetOperations).should().remove(contentKey(oldContentId), oldSessionId.toString());
      then(redisTemplate).should().delete(sessionKey(oldSessionId));
      then(redisTemplate).should().delete(watcherKey(watcherId));
      then(hashOperations).should().putAll(eq(sessionKey(session.id())), anyMap());
      then(valueOperations).should().set(watcherKey(watcherId), session.id().toString());
    }
  }

  @Nested
  @DisplayName("시청 퇴장 - leave()")
  class Leave {

    @Test
    @DisplayName("정상 퇴장 - 세션/역인덱스/ZSet 항목이 모두 정리되고, 종료된 세션을 반환한다")
    void success() {
      UUID watcherId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      Instant createdAt = Instant.now();

      given(valueOperations.get(watcherKey(watcherId))).willReturn(sessionId.toString());
      given(hashOperations.entries(sessionKey(sessionId)))
          .willReturn(sessionFields(watcherId, contentId, createdAt));
      given(hashOperations.get(sessionKey(sessionId), "contentId")).willReturn(contentId.toString());

      WatchingSession session = repository.leave(watcherId);

      assertThat(session.watcherId()).isEqualTo(watcherId);
      assertThat(session.contentId()).isEqualTo(contentId);
      then(zSetOperations).should().remove(contentKey(contentId), sessionId.toString());
      then(redisTemplate).should().delete(sessionKey(sessionId));
      then(redisTemplate).should().delete(watcherKey(watcherId));
    }

    @Test
    @DisplayName("보고 있는 세션이 없으면 WATCHING_SESSION_NOT_FOUND 예외")
    void fail_notFound() {
      UUID watcherId = UUID.randomUUID();
      given(valueOperations.get(watcherKey(watcherId))).willReturn(null);

      assertThatThrownBy(() -> repository.leave(watcherId))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.WATCHING_SESSION_NOT_FOUND));
    }
  }

  @Nested
  @DisplayName("특정 사용자의 현재 세션 조회 - findByWatcherId()")
  class FindByWatcherId {

    @Test
    @DisplayName("세션이 있으면 값을 채워서 반환한다")
    void found() {
      UUID watcherId = UUID.randomUUID();
      UUID contentId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

      given(valueOperations.get(watcherKey(watcherId))).willReturn(sessionId.toString());
      given(hashOperations.entries(sessionKey(sessionId)))
          .willReturn(sessionFields(watcherId, contentId, createdAt));

      Optional<WatchingSession> result = repository.findByWatcherId(watcherId);

      assertThat(result).isPresent();
      assertThat(result.get().id()).isEqualTo(sessionId);
      assertThat(result.get().watcherId()).isEqualTo(watcherId);
      assertThat(result.get().contentId()).isEqualTo(contentId);
      assertThat(result.get().createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("역인덱스가 없으면 빈 값을 반환한다")
    void notFound_noIndex() {
      UUID watcherId = UUID.randomUUID();
      given(valueOperations.get(watcherKey(watcherId))).willReturn(null);

      assertThat(repository.findByWatcherId(watcherId)).isEmpty();
    }

    @Test
    @DisplayName("역인덱스는 있는데 세션 원본이 지워졌으면 빈 값을 반환한다 (TTL 등으로 인한 불일치 방어)")
    void notFound_staleIndex() {
      UUID watcherId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      given(valueOperations.get(watcherKey(watcherId))).willReturn(sessionId.toString());
      given(hashOperations.entries(sessionKey(sessionId))).willReturn(Map.of());

      assertThat(repository.findByWatcherId(watcherId)).isEmpty();
    }
  }

  @Nested
  @DisplayName("콘텐츠별 시청 세션 목록 조회 - findByContentId()")
  class FindByContentId {

    @Test
    @DisplayName("커서 없음 + 내림차순 - reverseRangeWithScores로 최신순 조회한다")
    void noCursor_descending() {
      UUID contentId = UUID.randomUUID();
      UUID session1 = UUID.randomUUID();
      UUID session2 = UUID.randomUUID();
      Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
      Instant t2 = Instant.parse("2026-01-02T00:00:00Z");
      WatchingSessionSearchRequest request =
          new WatchingSessionSearchRequest(contentId, null, null, 2, "createdAt", "DESCENDING");

      given(zSetOperations.reverseRangeWithScores(contentKey(contentId), 0, 2)).willReturn(Set.of(
          new DefaultTypedTuple<>(session2.toString(), (double) t2.toEpochMilli()),
          new DefaultTypedTuple<>(session1.toString(), (double) t1.toEpochMilli())
      ));
      given(hashOperations.entries(sessionKey(session2)))
          .willReturn(sessionFields(UUID.randomUUID(), contentId, t2));
      given(hashOperations.entries(sessionKey(session1)))
          .willReturn(sessionFields(UUID.randomUUID(), contentId, t1));

      List<WatchingSession> result = repository.findByContentId(request);

      assertThat(result).extracting(WatchingSession::id).containsExactly(session2, session1);
    }

    @Test
    @DisplayName("커서 없음 + 오름차순 - rangeWithScores로 오래된 순 조회한다")
    void noCursor_ascending() {
      UUID contentId = UUID.randomUUID();
      UUID session1 = UUID.randomUUID();
      Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
      WatchingSessionSearchRequest request =
          new WatchingSessionSearchRequest(contentId, null, null, 5, "createdAt", "ASCENDING");

      given(zSetOperations.rangeWithScores(contentKey(contentId), 0, 5)).willReturn(Set.of(
          new DefaultTypedTuple<>(session1.toString(), (double) t1.toEpochMilli())
      ));
      given(hashOperations.entries(sessionKey(session1)))
          .willReturn(sessionFields(UUID.randomUUID(), contentId, t1));

      List<WatchingSession> result = repository.findByContentId(request);

      assertThat(result).extracting(WatchingSession::id).containsExactly(session1);
    }

    @Test
    @DisplayName("커서 있음 + 내림차순 - reverseRangeByScoreWithScores로 커서 이전 데이터를 조회한다")
    void withCursor_descending() {
      UUID contentId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      Instant cursorTime = Instant.parse("2026-01-02T00:00:00Z");
      Instant resultTime = Instant.parse("2026-01-01T00:00:00Z");
      WatchingSessionSearchRequest request = new WatchingSessionSearchRequest(
          contentId, cursorTime.toString(), UUID.randomUUID(), 5, "createdAt", "DESCENDING");

      given(zSetOperations.reverseRangeByScoreWithScores(
          contentKey(contentId), -Double.MAX_VALUE, cursorTime.toEpochMilli() - 1, 0, 6))
          .willReturn(Set.of(new DefaultTypedTuple<>(sessionId.toString(), (double) resultTime.toEpochMilli())));
      given(hashOperations.entries(sessionKey(sessionId)))
          .willReturn(sessionFields(UUID.randomUUID(), contentId, resultTime));

      List<WatchingSession> result = repository.findByContentId(request);

      assertThat(result).extracting(WatchingSession::id).containsExactly(sessionId);
    }

    @Test
    @DisplayName("커서 있음 + 오름차순 - rangeByScoreWithScores로 커서 이후 데이터를 조회한다")
    void withCursor_ascending() {
      UUID contentId = UUID.randomUUID();
      UUID sessionId = UUID.randomUUID();
      Instant cursorTime = Instant.parse("2026-01-01T00:00:00Z");
      Instant resultTime = Instant.parse("2026-01-02T00:00:00Z");
      WatchingSessionSearchRequest request = new WatchingSessionSearchRequest(
          contentId, cursorTime.toString(), UUID.randomUUID(), 5, "createdAt", "ASCENDING");

      given(zSetOperations.rangeByScoreWithScores(
          contentKey(contentId), cursorTime.toEpochMilli() + 1, Double.MAX_VALUE, 0, 6))
          .willReturn(Set.of(new DefaultTypedTuple<>(sessionId.toString(), (double) resultTime.toEpochMilli())));
      given(hashOperations.entries(sessionKey(sessionId)))
          .willReturn(sessionFields(UUID.randomUUID(), contentId, resultTime));

      List<WatchingSession> result = repository.findByContentId(request);

      assertThat(result).extracting(WatchingSession::id).containsExactly(sessionId);
    }

    @Test
    @DisplayName("커서 형식이 잘못되면 INVALID_CURSOR_FORMAT 예외")
    void invalidCursor_throwsException() {
      WatchingSessionSearchRequest request = new WatchingSessionSearchRequest(
          UUID.randomUUID(), "not-an-instant", UUID.randomUUID(), 5, "createdAt", "DESCENDING");

      assertThatThrownBy(() -> repository.findByContentId(request))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_CURSOR_FORMAT));
    }

    @Test
    @DisplayName("조회 결과가 없으면 빈 리스트를 반환한다")
    void empty() {
      UUID contentId = UUID.randomUUID();
      WatchingSessionSearchRequest request =
          new WatchingSessionSearchRequest(contentId, null, null, 5, "createdAt", "DESCENDING");

      given(zSetOperations.reverseRangeWithScores(contentKey(contentId), 0, 5)).willReturn(Set.of());

      assertThat(repository.findByContentId(request)).isEmpty();
    }

    @Test
    @DisplayName("ZSet엔 남아있는데 세션 원본이 지워진 항목은 결과에서 제외한다")
    void filtersOutStaleZSetEntries() {
      UUID contentId = UUID.randomUUID();
      UUID staleSessionId = UUID.randomUUID();
      Instant t = Instant.parse("2026-01-01T00:00:00Z");
      WatchingSessionSearchRequest request =
          new WatchingSessionSearchRequest(contentId, null, null, 5, "createdAt", "DESCENDING");

      given(zSetOperations.reverseRangeWithScores(contentKey(contentId), 0, 5)).willReturn(Set.of(
          new DefaultTypedTuple<>(staleSessionId.toString(), (double) t.toEpochMilli())
      ));
      given(hashOperations.entries(sessionKey(staleSessionId))).willReturn(Map.of());

      assertThat(repository.findByContentId(request)).isEmpty();
    }
  }

  @Nested
  @DisplayName("콘텐츠별 시청 세션 개수 - countByContentId()")
  class CountByContentId {

    @Test
    @DisplayName("ZSet 크기를 그대로 반환한다")
    void returnsCount() {
      UUID contentId = UUID.randomUUID();
      given(zSetOperations.zCard(contentKey(contentId))).willReturn(5L);

      assertThat(repository.countByContentId(contentId)).isEqualTo(5L);
    }

    @Test
    @DisplayName("ZSet이 없으면 (null) 0을 반환한다")
    void nullBecomesZero() {
      UUID contentId = UUID.randomUUID();
      given(zSetOperations.zCard(contentKey(contentId))).willReturn(null);

      assertThat(repository.countByContentId(contentId)).isZero();
    }
  }
}
