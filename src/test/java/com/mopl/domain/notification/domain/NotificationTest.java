package com.mopl.domain.notification.domain;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Notification 도메인 테스트")
class NotificationTest {

  @Test
  @DisplayName("성공: read() 호출 시 isRead가 true가 된다")
  void read_marksAsRead() {
    Notification notification = Notification.builder()
        .receiverId(UUID.randomUUID())
        .type(NotificationType.PLAYLIST_UPDATED)
        .title("플레이리스트 업데이트")
        .content("새 콘텐츠가 추가되었습니다.")
        .createdAt(Instant.now())
        .build();

    assertThat(notification.isRead()).isFalse();

    notification.read();

    assertThat(notification.isRead()).isTrue();
  }
}
