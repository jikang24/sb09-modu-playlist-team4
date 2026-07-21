package com.mopl.domain.notification.repository;

import com.mopl.domain.notification.domain.NotificationFailureLog;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationFailureLogRepository extends JpaRepository<NotificationFailureLog, UUID> {
}