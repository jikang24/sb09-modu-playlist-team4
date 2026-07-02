package com.mopl.domain.notification.repository;

import com.mopl.domain.notification.domain.Notification;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
    NotificationRepositoryCustom {

  long countByReceiverIdAndIsReadFalse(UUID receiverId);
}
