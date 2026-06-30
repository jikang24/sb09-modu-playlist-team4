package com.mopl.domain.notification.repository;

import com.mopl.domain.notification.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID>,
    NotificationRepositoryCustom {

  List<Notification> findByReceiverIdOrderByCreatedAtDesc(UUID receiverId);

  long countByReceiverIdAndIsReadFalse(UUID receiverId);
}
