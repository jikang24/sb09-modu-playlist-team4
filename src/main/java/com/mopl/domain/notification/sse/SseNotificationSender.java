package com.mopl.domain.notification.sse;

import com.mopl.domain.notification.dto.NotificationDto;
import java.util.UUID;

public interface SseNotificationSender {

  void send(UUID receiverId, NotificationDto notification);
}
