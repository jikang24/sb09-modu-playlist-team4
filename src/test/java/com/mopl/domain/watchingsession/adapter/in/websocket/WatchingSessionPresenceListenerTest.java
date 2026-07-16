package com.mopl.domain.watchingsession.adapter.in.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.mopl.domain.watchingsession.application.port.in.WatchingSessionUseCase;
import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.global.jwt.JwtClaims;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionPresenceListener 테스트")
class WatchingSessionPresenceListenerTest {

  @InjectMocks
  private WatchingSessionPresenceListener listener;

  @Mock
  private WatchingSessionUseCase watchingSessionUseCase;

  private final UUID watcherId = UUID.randomUUID();
  private final UUID contentId = UUID.randomUUID();
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

  private WatchingSessionDto dto(UUID sessionId) {
    return new WatchingSessionDto(sessionId, Instant.now(), null, null);
  }

  @Test
  @DisplayName("watch 토픽 구독 시 입장 처리된다")
  void handleSubscribe_watchTopic_enters() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", claims);
    given(watchingSessionUseCase.enter(watcherId, contentId)).willReturn(dto(UUID.randomUUID()));

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    then(watchingSessionUseCase).should().enter(watcherId, contentId);
  }

  @Test
  @DisplayName("chat 토픽 구독은 입장 처리와 무관하다")
  void handleSubscribe_chatTopic_doesNothing() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/chat", "session-1", "sub-1", claims);

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    then(watchingSessionUseCase).should(never()).enter(any(), any());
  }

  @Test
  @DisplayName("인증 정보(claims)가 없으면 입장 처리하지 않는다")
  void handleSubscribe_noClaims_doesNothing() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", null);

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    then(watchingSessionUseCase).should(never()).enter(any(), any());
  }

  @Test
  @DisplayName("구독했던 watch 토픽을 구독 해제하면 자기 자신의 watchingSessionId로 조건부 퇴장을 요청한다")
  void handleUnsubscribe_matchingSubscription_leavesWithOwnSessionId() {
    UUID watchingSessionId = UUID.randomUUID();
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", claims);
    given(watchingSessionUseCase.enter(watcherId, contentId)).willReturn(dto(watchingSessionId));
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    StompHeaderAccessor unsubscribe = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "session-1", "sub-1", claims);

    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribe)));

    then(watchingSessionUseCase).should().leaveIfCurrent(watcherId, watchingSessionId);
  }

  @Test
  @DisplayName("추적하지 않은 구독(예: chat) 해제는 퇴장 처리하지 않는다")
  void handleUnsubscribe_untrackedSubscription_doesNothing() {
    StompHeaderAccessor unsubscribe = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "session-1", "sub-99", claims);

    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribe)));

    then(watchingSessionUseCase).should(never()).leaveIfCurrent(any(), any());
  }

  @Test
  @DisplayName("watch 구독 중이던 세션이 끊기면 자기 자신의 watchingSessionId로 조건부 퇴장을 요청한다")
  void handleDisconnect_wasWatching_leavesWithOwnSessionId() {
    UUID watchingSessionId = UUID.randomUUID();
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", claims);
    given(watchingSessionUseCase.enter(watcherId, contentId)).willReturn(dto(watchingSessionId));
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    StompHeaderAccessor disconnect = createAccessor(StompCommand.DISCONNECT, null, "session-1", null, claims);

    listener.handleDisconnect(
        new SessionDisconnectEvent(this, toMessage(disconnect), "session-1", CloseStatus.NORMAL));

    then(watchingSessionUseCase).should().leaveIfCurrent(watcherId, watchingSessionId);
  }

  @Test
  @DisplayName("watch 구독이 없던 세션이 끊기면 퇴장 처리하지 않는다")
  void handleDisconnect_wasNotWatching_doesNothing() {
    StompHeaderAccessor disconnect = createAccessor(StompCommand.DISCONNECT, null, "session-2", null, claims);

    listener.handleDisconnect(
        new SessionDisconnectEvent(this, toMessage(disconnect), "session-2", CloseStatus.NORMAL));

    then(watchingSessionUseCase).should(never()).leaveIfCurrent(any(), any());
  }

  @Test
  @DisplayName("예기치 못한 예외가 나도 조용히 무시되고 전파되지 않는다")
  void handleUnsubscribe_unexpectedException_swallowed() {
    UUID watchingSessionId = UUID.randomUUID();
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", claims);
    given(watchingSessionUseCase.enter(watcherId, contentId)).willReturn(dto(watchingSessionId));
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    willThrow(new RuntimeException("redis down"))
        .given(watchingSessionUseCase).leaveIfCurrent(watcherId, watchingSessionId);

    StompHeaderAccessor unsubscribe = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "session-1", "sub-1", claims);

    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribe)));

    then(watchingSessionUseCase).should().leaveIfCurrent(watcherId, watchingSessionId);
  }

  @Test
  @DisplayName("탭 전환 시나리오 - 뒤늦은 이전 탭의 이벤트는 자기 자신의 세션ID로만 조건부 퇴장을 요청하고, 새 탭의 세션ID와는 무관하다")
  void handleUnsubscribe_tabSwitch_onlyReferencesOwnSessionId() {
    UUID contentX = UUID.randomUUID();
    UUID contentY = UUID.randomUUID();
    UUID watchingSessionIdX = UUID.randomUUID();
    UUID watchingSessionIdY = UUID.randomUUID();

    // 탭A: 콘텐츠X 구독 (웹소켓 세션 "tabA")
    StompHeaderAccessor subscribeA = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentX + "/watch", "tabA", "sub-1", claims);
    given(watchingSessionUseCase.enter(watcherId, contentX)).willReturn(dto(watchingSessionIdX));
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribeA)));

    // 탭B(같은 유저, 다른 웹소켓 연결): 콘텐츠Y 구독
    // 실제 서버에서는 enter()가 내부적으로 탭A의 Redis 세션을 정리하고 Y로 교체함
    StompHeaderAccessor subscribeB = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentY + "/watch", "tabB", "sub-1", claims);
    given(watchingSessionUseCase.enter(watcherId, contentY)).willReturn(dto(watchingSessionIdY));
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribeB)));

    // 탭A가 뒤늦게 구독 해제됨 (네트워크 지연 등으로)
    StompHeaderAccessor unsubscribeA = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "tabA", "sub-1", claims);
    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribeA)));

    // 탭A는 자기 자신의 세션(X)에 대해서만 조건부 퇴장을 요청해야 하고, 탭B의 세션(Y)은 절대 건드리면 안 된다
    then(watchingSessionUseCase).should().leaveIfCurrent(watcherId, watchingSessionIdX);
    then(watchingSessionUseCase).should(never()).leaveIfCurrent(watcherId, watchingSessionIdY);
  }
}