package com.mopl.domain.notification.sse;

import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.global.dto.DirectMessageDto;
import java.util.UUID;

public interface SseNotificationSender {

  void send(UUID receiverId, NotificationDto notification);

  /** 비활성 대화(receiver가 지금 그 대화방을 구독 중이지 않은 경우)의 DM을 name: "direct-messages" 이벤트로 전달한다 */
  void sendDirectMessage(UUID receiverId, DirectMessageDto directMessage);
}
