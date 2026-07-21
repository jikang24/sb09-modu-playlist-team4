package com.mopl.domain.watchingsession.repository;

import com.mopl.domain.watchingsession.domain.WatchingSession;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Repository;

/**
 * 시청 세션 Redis 저장소
 *
 *  watching:session:{sessionId}          Hash   - {watcherId, contentId, createdAt} (세션 원본 데이터, SESSION_TTL 만료)
 *  watching:content:{contentId}:sessions ZSet   - member=sessionId, score=createdAt(epochMilli) (콘텐츠별 목록 + 커서 페이지네이션용 정렬 인덱스)
 *  watching:watcher:{watcherId}          String - sessionId (유저 -> 현재 세션 역인덱스, 한 사람당 최대 1개, SESSION_TTL 만료)
 *  watching:content:index                Set    - member=contentId (세션이 한 번이라도 생긴 콘텐츠 목록, 정리 배치가 순회할 때 사용)
 *
 * disconnect 이벤트(STOMP UNSUBSCRIBE/DISCONNECT)가 서버 크래시·강제 종료·배포 중 인스턴스 교체로
 * 유실되면 정상 종료 경로를 못 타므로, Hash/String엔 SESSION_TTL을 걸어 자연 소멸시킨다.
 * ZSet은 같은 콘텐츠의 여러 세션이 공유하는 키라 항목 단위 TTL이 불가능해 소멸하지 않는데,
 * 그 고아 멤버는 {@link #cleanupOrphanSessions()}가 주기적으로 스캔해서 제거한다.
 */
@Repository
@RequiredArgsConstructor
public class WatchingSessionRedisRepository implements WatchingSessionRepository {

  private static final String SESSION_KEY_PREFIX = "watching:session:";
  private static final String WATCHER_INDEX_PREFIX = "watching:watcher:";
  private static final String CONTENT_INDEX_SET_KEY = "watching:content:index";

  // 하트비트 없이 disconnect 유실 시 자연 소멸시키는 백스톱 TTL - 정상적인 한 편 시청 시간보다 충분히 길게 잡는다
  private static final Duration SESSION_TTL = Duration.ofHours(12);

  private final StringRedisTemplate redisTemplate;

  @Override
  public WatchingSession enter(UUID watcherId, UUID contentId) {
    // 한 사람은 동시에 하나만 시청 가능 - 기존에 보던 게 있으면 먼저 정리
    leaveIfPresent(watcherId);

    WatchingSession session = WatchingSession.create(watcherId, contentId);

    Map<String, String> fields = new HashMap<>();
    fields.put("watcherId", watcherId.toString());
    fields.put("contentId", contentId.toString());
    fields.put("createdAt", session.createdAt().toString());
    redisTemplate.opsForHash().putAll(sessionKey(session.id()), fields);
    redisTemplate.expire(sessionKey(session.id()), SESSION_TTL);

    redisTemplate.opsForZSet().add(
        contentIndexKey(contentId), session.id().toString(), session.createdAt().toEpochMilli());
    redisTemplate.opsForValue().set(watcherIndexKey(watcherId), session.id().toString(), SESSION_TTL);
    redisTemplate.opsForSet().add(CONTENT_INDEX_SET_KEY, contentId.toString());

    return session;
  }

  @Override
  public WatchingSession leave(UUID watcherId) {
    String sessionId = redisTemplate.opsForValue().get(watcherIndexKey(watcherId));
    if (sessionId == null) {
      throw new MoplException(ErrorCode.WATCHING_SESSION_NOT_FOUND);
    }
    WatchingSession session = findSession(sessionId)
        .orElseThrow(() -> new MoplException(ErrorCode.WATCHING_SESSION_NOT_FOUND));
    removeSession(watcherId, sessionId);
    return session;
  }

  @Override
  public Optional<WatchingSession> leaveIfCurrent(UUID watcherId, UUID sessionId) {
    String currentSessionId = redisTemplate.opsForValue().get(watcherIndexKey(watcherId));
    if (currentSessionId == null || !currentSessionId.equals(sessionId.toString())) {
      return Optional.empty(); // 이미 다른 세션으로 교체됐거나 이미 종료됨 - 늦게 도착한 이벤트이므로 무시
    }
    Optional<WatchingSession> session = findSession(currentSessionId);
    if (session.isEmpty()) {
      return Optional.empty();
    }
    removeSession(watcherId, currentSessionId);
    return session;
  }

  @Override
  public Optional<WatchingSession> findByWatcherId(UUID watcherId) {
    String sessionId = redisTemplate.opsForValue().get(watcherIndexKey(watcherId));
    if (sessionId == null) {
      return Optional.empty();
    }
    return findSession(sessionId);
  }

  @Override
  public List<WatchingSession> findByContentId(WatchingSessionSearchRequest request) {
    String key = contentIndexKey(request.contentId());
    boolean isAscending = "ASCENDING".equalsIgnoreCase(request.sortDirection());
    int fetchSize = request.limit() + 1; // hasNext 판단용으로 하나 더 조회

    Set<ZSetOperations.TypedTuple<String>> tuples;
    if (request.cursor() == null) {
      tuples = isAscending
          ? redisTemplate.opsForZSet().rangeWithScores(key, 0, fetchSize - 1)
          : redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, fetchSize - 1);
    } else {
      double cursorScore = parseCursor(request.cursor());
      tuples = isAscending
          ? redisTemplate.opsForZSet()
              .rangeByScoreWithScores(key, cursorScore + 1, Double.MAX_VALUE, 0, fetchSize)
          : redisTemplate.opsForZSet()
              .reverseRangeByScoreWithScores(key, -Double.MAX_VALUE, cursorScore - 1, 0, fetchSize);
    }

    if (tuples == null || tuples.isEmpty()) {
      return List.of();
    }

    Comparator<TypedTuple<String>> byScore =
        Comparator.comparing(ZSetOperations.TypedTuple::getScore);

    return tuples.stream()
        .sorted(isAscending ? byScore : byScore.reversed())
        .map(tuple -> findSession(tuple.getValue()))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
  }

  @Override
  public long countByContentId(UUID contentId) {
    Long count = redisTemplate.opsForZSet().zCard(contentIndexKey(contentId));
    return count != null ? count : 0L;
  }

  @Override
  public long cleanupOrphanSessions() {
    Set<String> contentIds = redisTemplate.opsForSet().members(CONTENT_INDEX_SET_KEY);
    if (contentIds == null || contentIds.isEmpty()) {
      return 0L;
    }

    long removedCount = 0L;
    for (String contentId : contentIds) {
      removedCount += cleanupOrphanSessionsForContent(contentId);
    }
    return removedCount;
  }

  private long cleanupOrphanSessionsForContent(String contentId) {
    String contentIndexKey = contentIndexKey(UUID.fromString(contentId));
    Set<String> sessionIds = redisTemplate.opsForZSet().range(contentIndexKey, 0, -1);
    if (sessionIds == null) {
      return 0L;
    }

    long removedCount = 0L;
    for (String sessionId : sessionIds) {
      if (Boolean.FALSE.equals(redisTemplate.hasKey(sessionKey(sessionId)))) {
        redisTemplate.opsForZSet().remove(contentIndexKey, sessionId);
        removedCount++;
      }
    }

    Long remaining = redisTemplate.opsForZSet().zCard(contentIndexKey);
    if (remaining != null && remaining == 0) {
      redisTemplate.opsForSet().remove(CONTENT_INDEX_SET_KEY, contentId);
    }
    return removedCount;
  }

  private void leaveIfPresent(UUID watcherId) {
    String sessionId = redisTemplate.opsForValue().get(watcherIndexKey(watcherId));
    if (sessionId != null) {
      removeSession(watcherId, sessionId);
    }
  }

  private void removeSession(UUID watcherId, String sessionId) {
    Object contentIdValue = redisTemplate.opsForHash().get(sessionKey(sessionId), "contentId");
    if (contentIdValue != null) {
      redisTemplate.opsForZSet().remove(contentIndexKey(UUID.fromString((String) contentIdValue)), sessionId);
    }
    redisTemplate.delete(sessionKey(sessionId));
    redisTemplate.delete(watcherIndexKey(watcherId));
  }

  private Optional<WatchingSession> findSession(String sessionId) {
    Map<Object, Object> fields = redisTemplate.opsForHash().entries(sessionKey(sessionId));
    if (fields.isEmpty()) {
      return Optional.empty(); // ZSet엔 남아있는데 세션 원본이 이미 지워진 경우 (TTL 등) - 방어적으로 스킵
    }
    return Optional.of(new WatchingSession(
        UUID.fromString(sessionId),
        UUID.fromString((String) fields.get("watcherId")),
        UUID.fromString((String) fields.get("contentId")),
        Instant.parse((String) fields.get("createdAt"))
    ));
  }

  private double parseCursor(String cursor) {
    try {
      return Instant.parse(cursor).toEpochMilli();
    } catch (DateTimeParseException e) {
      throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
    }
  }

  private String sessionKey(UUID sessionId) {
    return sessionKey(sessionId.toString());
  }

  private String sessionKey(String sessionId) {
    return SESSION_KEY_PREFIX + sessionId;
  }

  private String contentIndexKey(UUID contentId) {
    return "watching:content:" + contentId + ":sessions";
  }

  private String watcherIndexKey(UUID watcherId) {
    return WATCHER_INDEX_PREFIX + watcherId;
  }
}