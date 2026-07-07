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
                "권한 변경 안내",
                "회원님의 권한이 " + event.newRole() + "(으)로 변경되었습니다."
        ));
    }
}