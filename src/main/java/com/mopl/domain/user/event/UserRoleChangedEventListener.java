package com.mopl.domain.user.event;

import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.jwt.AuthTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRoleChangedEventListener {

    private final NotificationEventPublisher notificationEventPublisher;
    private final AuthTokenService authTokenService;

    @Transactional
    @EventListener
    public void onRoleChanged(UserRoleChangedEvent event) {
        log.info("publisher class = {}", notificationEventPublisher.getClass());
        NotificationRequestedEvent notificationEvent = new NotificationRequestedEvent(
                event.userId(),
                NotificationType.ROLE_CHANGED.name(),
                "내 권한이 변경되었어요.",
                "내 권한이 [" + event.oldRole() + "]에서 [" + event.newRole() + "]로 변경되었어요."
        );

        log.info("권한 변경 알림 요청 발행 - userId={}, oldRole={}, newRole={}, eventId={}",
                event.userId(), event.oldRole(), event.newRole(), notificationEvent.eventId());
        notificationEventPublisher.publish(notificationEvent);

        try {
            authTokenService.forceLogoutByUserId(event.userId());
        } catch (Exception e) {
            log.warn("권한 변경 후 강제 로그아웃 실패 - userId={}", event.userId(), e);
        }
    }
}
