package com.mopl.domain.user.event;

import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserRoleChangedEventListener {

    private final NotificationEventPublisher notificationEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoleChanged(UserRoleChangedEvent event) {
        notificationEventPublisher.publish(new NotificationRequestedEvent(
                event.userId(),
                NotificationType.ROLE_CHANGED.name(),
                "내 권한이 변경되었어요.",
                "내 권한이 [" + event.oldRole() + "]에서 [" + event.newRole() + "]로 변경되었어요."
        ));
    }
}