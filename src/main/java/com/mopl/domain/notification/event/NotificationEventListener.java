package com.mopl.domain.notification.event;

import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.domain.user.event.UserRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

  private final NotificationService notificationService;

  @EventListener
  public void handle(UserRoleChangedEvent event) {
    notificationService.send(
        event.userId(),
        NotificationType.ROLE_CHANGED,
        "권한이 변경되었습니다.",
        "회원님의 권한이 " + event.newRole().name() + "(으)로 변경되었습니다."
    );
  }
}
