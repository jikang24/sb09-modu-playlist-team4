package com.mopl.domain.notification.repository;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.dto.NotificationSearchRequest;
import java.util.List;
import java.util.UUID;

public interface NotificationRepositoryCustom {

  List<Notification> findUnreadByReceiverWithCursor(UUID receiverId, NotificationSearchRequest request);

  List<Notification> findByReceiverIdAfter(UUID receiverId, UUID lastNotificationId);
}
