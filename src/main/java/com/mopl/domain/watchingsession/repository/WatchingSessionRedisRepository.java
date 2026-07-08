package com.mopl.domain.watchingsession.repository;

import com.mopl.domain.watchingsession.domain.WatchingSession;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
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
 *  watching:session:{sessionId}          Hash   - {watcherId, contentId, createdAt} (세션 원본 데이터)
 *  watching:content:{contentId}:sessions ZSet   - member=sessionId, score=createdAt(epochMilli) (콘텐츠별 목록 + 커서 페이지네이션용 정렬 인덱스)
 *  watching:watcher:{watcherId}          String - sessionId (유저 -> 현재 세션 역인덱스, 한 사람당 최대 1개)
 */
@Repository
@RequiredArgsConstructor
public class WatchingSessionRedisRepository implements WatchingSessionRepository {

  private static final String SESSION_KEY_PREFIX = "watching:session:";
  private static final String WATCHER_INDEX_PREFIX = "watching:watcher:";

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

    redisTemplate.opsForZSet().add(
        contentIndexKey(contentId), session.id().toString(), session.createdAt().toEpochMilli());
    redisTemplate.opsForValue().set(watcherIndexKey(watcherId), session.id().toString());

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