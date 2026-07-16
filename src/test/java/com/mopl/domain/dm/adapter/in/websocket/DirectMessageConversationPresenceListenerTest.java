package com.mopl.domain.dm.adapter.in.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.global.jwt.JwtClaims;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@DisplayName("DirectMessageConversationPresenceListener 테스트")
class DirectMessageConversationPresenceListenerTest {

  private final DirectMessageConversationPresenceListener listener =
      new DirectMessageConversationPresenceListener();

  private final UUID watcherId = UUID.randomUUID();
  private final UUID conversationId = UUID.randomUUID();
  private final JwtClaims claims = JwtClaims.builder()
      .userId(watcherId)
      .email("watcher@email.com")
      .role("USER")
      .tokenId(UUID.randomUUID().toString())
      .build();

  private StompHeaderAccessor createAccessor(
      StompCommand command, String destination, String sessionId, String subscriptionId, JwtClaims claims) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
    accessor.setSessionId(sessionId);
    accessor.setSessionAttributes(new HashMap<>());
    if (claims != null) {
      accessor.getSessionAttributes().put("claims", claims);
    }
    if (destination != null) {
      accessor.setDestination(destination);
    }
    if (subscriptionId != null) {
      accessor.setSubscriptionId(subscriptionId);
    }
    return accessor;
  }

  private Message<byte[]> toMessage(StompHeaderAccessor accessor) {
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  @Test
  @DisplayName("대화방 구독 시 활성 상태로 등록된다")
  void handleSubscribe_marksActive() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/conversations/" + conversationId + "/direct-messages",
        "session-1", "sub-1", claims);

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    assertThat(listener.isActive(watcherId, conversationId)).isTrue();
  }

  @Test
  @DisplayName("다른 토픽(watch 등) 구독은 활성 상태와 무관하다")
  void handleSubscribe_otherTopic_doesNothing() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + UUID.randomUUID() + "/watch",
        "session-1", "sub-1", claims);

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    assertThat(listener.isActive(watcherId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("인증 정보(claims)가 없으면 활성 처리하지 않는다")
  void handleSubscribe_noClaims_doesNothing() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/conversations/" + conversationId + "/direct-messages",
        "session-1", "sub-1", null);

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    assertThat(listener.isActive(watcherId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("구독 해제하면 비활성 상태로 전환된다")
  void handleUnsubscribe_marksInactive() {
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/conversations/" + conversationId + "/direct-messages",
        "session-1", "sub-1", claims);
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    StompHeaderAccessor unsubscribe = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "session-1", "sub-1", claims);
    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribe)));

    assertThat(listener.isActive(watcherId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("추적하지 않은 구독 해제는 다른 활성 상태에 영향을 주지 않는다")
  void handleUnsubscribe_untrackedSubscription_doesNothing() {
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/conversations/" + conversationId + "/direct-messages",
        "session-1", "sub-1", claims);
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    StompHeaderAccessor unsubscribe = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "session-1", "sub-99", claims);
    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribe)));

    assertThat(listener.isActive(watcherId, conversationId)).isTrue();
  }

  @Test
  @DisplayName("연결이 끊기면 그 세션이 구독하던 대화방들이 모두 비활성 상태로 전환된다")
  void handleDisconnect_marksInactive() {
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/conversations/" + conversationId + "/direct-messages",
        "session-1", "sub-1", claims);
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    StompHeaderAccessor disconnect = createAccessor(StompCommand.DISCONNECT, null, "session-1", null, claims);
    listener.handleDisconnect(
        new SessionDisconnectEvent(this, toMessage(disconnect), "session-1", CloseStatus.NORMAL));

    assertThat(listener.isActive(watcherId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("같은 유저가 여러 탭에서 같은 대화방을 구독 중이면, 한 탭이 나가도 다른 탭이 살아있는 한 여전히 활성 상태다")
  void multipleSessions_sameConversation_remainsActiveUntilAllLeave() {
    StompHeaderAccessor tabA = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/conversations/" + conversationId + "/direct-messages",
        "tabA", "sub-1", claims);
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(tabA)));

    StompHeaderAccessor tabB = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/conversations/" + conversationId + "/direct-messages",
        "tabB", "sub-1", claims);
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(tabB)));

    StompHeaderAccessor disconnectA = createAccessor(StompCommand.DISCONNECT, null, "tabA", null, claims);
    listener.handleDisconnect(
        new SessionDisconnectEvent(this, toMessage(disconnectA), "tabA", CloseStatus.NORMAL));

    assertThat(listener.isActive(watcherId, conversationId)).isTrue();

    StompHeaderAccessor disconnectB = createAccessor(StompCommand.DISCONNECT, null, "tabB", null, claims);
    listener.handleDisconnect(
        new SessionDisconnectEvent(this, toMessage(disconnectB), "tabB", CloseStatus.NORMAL));

    assertThat(listener.isActive(watcherId, conversationId)).isFalse();
  }

  @Test
  @DisplayName("구독한 적 없는 (유저, 대화방) 조합은 비활성 상태다")
  void isActive_neverSubscribed_isFalse() {
    assertThat(listener.isActive(watcherId, conversationId)).isFalse();
  }
}
