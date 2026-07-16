package com.mopl.domain.dm.adapter.in.websocket;

import com.mopl.global.jwt.JwtClaims;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;


@Slf4j
@Component
public class DirectMessageConversationPresenceListener {

  private static final Pattern DM_DESTINATION =
      Pattern.compile("^/sub/conversations/([^/]+)/direct-messages$");

  
  private final Map<String, Map<String, ActiveConversation>> sessionSubscriptions =
      new ConcurrentHashMap<>();

  
  private final Map<ActiveConversation, Integer> activeCounts = new ConcurrentHashMap<>();

  private record ActiveConversation(UUID watcherId, UUID conversationId) {}

  @EventListener
  public void handleSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    UUID conversationId = extractConversationId(accessor.getDestination());
    if (conversationId == null) {
      return;
    }

    UUID watcherId = extractWatcherId(accessor);
    if (watcherId == null) {
      log.warn("[DirectMessagePresence] 구독 추적 실패 - 인증 정보 없음, conversationId: {}", conversationId);
      return;
    }

    ActiveConversation active = new ActiveConversation(watcherId, conversationId);
    sessionSubscriptions
        .computeIfAbsent(accessor.getSessionId(), ignored -> new ConcurrentHashMap<>())
        .put(accessor.getSubscriptionId(), active);
    activeCounts.merge(active, 1, Integer::sum);
    log.debug("[DirectMessagePresence] 활성 대화 등록 - watcherId: {}, conversationId: {}",
        watcherId, conversationId);
  }

  @EventListener
  public void handleUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
    removeSubscription(accessor.getSessionId(), accessor.getSubscriptionId());
  }

  @EventListener
  public void handleDisconnect(SessionDisconnectEvent event) {
    Map<String, ActiveConversation> subs = sessionSubscriptions.remove(event.getSessionId());
    if (subs == null) {
      return;
    }
    subs.values().forEach(this::decrement);
  }

  /** 해당 유저가 지금 이 대화방을 실시간으로 구독 중인지(=활성 대화) 여부 */
  public boolean isActive(UUID watcherId, UUID conversationId) {
    return activeCounts.containsKey(new ActiveConversation(watcherId, conversationId));
  }

  private void removeSubscription(String sessionId, String subscriptionId) {
    Map<String, ActiveConversation> subs = sessionSubscriptions.get(sessionId);
    if (subs == null) {
      return;
    }

    ActiveConversation active = subs.remove(subscriptionId);
    if (active == null) {
      return; 
    }
    if (subs.isEmpty()) {
      sessionSubscriptions.remove(sessionId);
    }
    decrement(active);
  }

  private void decrement(ActiveConversation active) {
    activeCounts.computeIfPresent(active, (key, count) -> count <= 1 ? null : count - 1);
  }

  private UUID extractConversationId(String destination) {
    if (destination == null) {
      return null;
    }
    Matcher matcher = DM_DESTINATION.matcher(destination);
    if (!matcher.matches()) {
      return null;
    }
    try {
      return UUID.fromString(matcher.group(1));
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private UUID extractWatcherId(StompHeaderAccessor accessor) {
    if (accessor.getSessionAttributes() == null) {
      return null;
    }
    Object claims = accessor.getSessionAttributes().get("claims");
    if (!(claims instanceof JwtClaims jwtClaims)) {
      return null;
    }
    return jwtClaims.getUserId();
  }
}
