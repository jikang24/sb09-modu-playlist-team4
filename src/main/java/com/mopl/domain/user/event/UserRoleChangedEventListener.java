package com.mopl.domain.user.event;

import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.jwt.AuthTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRoleChangedEventListener {

    private final NotificationEventPublisher notificationEventPublisher;
    private final AuthTokenService authTokenService;

    @EventListener
    @Transactional
    public void onRoleChanged(UserRoleChangedEvent event) {

        NotificationRequestedEvent notificationEvent =
            new NotificationRequestedEvent(
                event.userId(),
                NotificationType.ROLE_CHANGED.name(),
                "내 권한이 변경되었어요.",
                "내 권한이 [" + event.oldRole() + "]에서 ["
                    + event.newRole() + "]로 변경되었어요."
            );

        notificationEventPublisher.publish(notificationEvent);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void logout(UserRoleChangedEvent event) {
        authTokenService.forceLogoutByUserId(event.userId());
    }
}