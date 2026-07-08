package com.mopl.domain.watchingsession.adapter.in.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.domain.watchingsession.service.WatchingSessionService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
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
  private WatchingSessionService watchingSessionService;

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

  @Test
  @DisplayName("watch 토픽 구독 시 입장 처리된다")
  void handleSubscribe_watchTopic_enters() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", claims);
    given(watchingSessionService.enter(watcherId, contentId))
        .willReturn(new WatchingSessionDto(UUID.randomUUID(), Instant.now(), null, null));

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    then(watchingSessionService).should().enter(watcherId, contentId);
  }

  @Test
  @DisplayName("chat 토픽 구독은 입장 처리와 무관하다")
  void handleSubscribe_chatTopic_doesNothing() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/chat", "session-1", "sub-1", claims);

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    then(watchingSessionService).should(never()).enter(any(), any());
  }

  @Test
  @DisplayName("인증 정보(claims)가 없으면 입장 처리하지 않는다")
  void handleSubscribe_noClaims_doesNothing() {
    StompHeaderAccessor accessor = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", null);

    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(accessor)));

    then(watchingSessionService).should(never()).enter(any(), any());
  }

  @Test
  @DisplayName("구독했던 watch 토픽을 구독 해제하면 퇴장 처리된다")
  void handleUnsubscribe_matchingSubscription_leaves() {
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", claims);
    given(watchingSessionService.enter(watcherId, contentId))
        .willReturn(new WatchingSessionDto(UUID.randomUUID(), Instant.now(), null, null));
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    StompHeaderAccessor unsubscribe = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "session-1", "sub-1", claims);

    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribe)));

    then(watchingSessionService).should().leave(watcherId);
  }

  @Test
  @DisplayName("추적하지 않은 구독(예: chat) 해제는 퇴장 처리하지 않는다")
  void handleUnsubscribe_untrackedSubscription_doesNothing() {
    StompHeaderAccessor unsubscribe = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "session-1", "sub-99", claims);

    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribe)));

    then(watchingSessionService).should(never()).leave(any());
  }

  @Test
  @DisplayName("watch 구독 중이던 세션이 끊기면 퇴장 처리된다")
  void handleDisconnect_wasWatching_leaves() {
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", claims);
    given(watchingSessionService.enter(watcherId, contentId))
        .willReturn(new WatchingSessionDto(UUID.randomUUID(), Instant.now(), null, null));
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    StompHeaderAccessor disconnect = createAccessor(StompCommand.DISCONNECT, null, "session-1", null, claims);

    listener.handleDisconnect(
        new SessionDisconnectEvent(this, toMessage(disconnect), "session-1", CloseStatus.NORMAL));

    then(watchingSessionService).should().leave(watcherId);
  }

  @Test
  @DisplayName("watch 구독이 없던 세션이 끊기면 퇴장 처리하지 않는다")
  void handleDisconnect_wasNotWatching_doesNothing() {
    StompHeaderAccessor disconnect = createAccessor(StompCommand.DISCONNECT, null, "session-2", null, claims);

    listener.handleDisconnect(
        new SessionDisconnectEvent(this, toMessage(disconnect), "session-2", CloseStatus.NORMAL));

    then(watchingSessionService).should(never()).leave(any());
  }

  @Test
  @DisplayName("이미 퇴장된 세션(WATCHING_SESSION_NOT_FOUND)은 예외 없이 무시된다")
  void handleUnsubscribe_alreadyLeft_swallowsException() {
    StompHeaderAccessor subscribe = createAccessor(
        StompCommand.SUBSCRIBE, "/sub/contents/" + contentId + "/watch", "session-1", "sub-1", claims);
    given(watchingSessionService.enter(watcherId, contentId))
        .willReturn(new WatchingSessionDto(UUID.randomUUID(), Instant.now(), null, null));
    listener.handleSubscribe(new SessionSubscribeEvent(this, toMessage(subscribe)));

    willThrow(new MoplException(ErrorCode.WATCHING_SESSION_NOT_FOUND))
        .given(watchingSessionService).leave(watcherId);

    StompHeaderAccessor unsubscribe = createAccessor(
        StompCommand.UNSUBSCRIBE, null, "session-1", "sub-1", claims);

    listener.handleUnsubscribe(new SessionUnsubscribeEvent(this, toMessage(unsubscribe)));

    then(watchingSessionService).should().leave(watcherId);
  }
}