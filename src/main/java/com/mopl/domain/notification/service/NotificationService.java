package com.mopl.domain.notification.service;

import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.dto.NotificationSearchRequest;
import com.mopl.global.response.CursorPageResponse;
import java.util.UUID;

public interface NotificationService {

  NotificationDto send(UUID receiverId, NotificationType type, String title, String content);

  CursorPageResponse<NotificationDto> findMyNotifications(NotificationSearchRequest request);

  void read(UUID notificationId);
}
