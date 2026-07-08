package com.mopl.domain.notification.dto;

import com.mopl.domain.notification.domain.Notification;
import com.mopl.domain.notification.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationDto / NotificationLevel 테스트")
class NotificationDtoTest {

  @Test
  @DisplayName("성공: Notification 엔티티를 DTO로 변환한다")
  void from_mapsEntityToDto() {
    UUID id = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
    Notification notification = Notification.builder()
        .id(id)
        .receiverId(receiverId)
        .type(NotificationType.FOLLOW)
        .title("새 팔로워")
        .content("누군가 팔로우했습니다.")
        .createdAt(createdAt)
        .build();

    NotificationDto dto = NotificationDto.from(notification);

    assertThat(dto.id()).isEqualTo(id);
    assertThat(dto.receiverId()).isEqualTo(receiverId);
    assertThat(dto.createdAt()).isEqualTo(createdAt);
    assertThat(dto.title()).isEqualTo("새 팔로워");
    assertThat(dto.content()).isEqualTo("누군가 팔로우했습니다.");
    assertThat(dto.level()).isEqualTo(NotificationLevel.INFO);
  }

  @ParameterizedTest(name = "type={0} → level 매핑")
  @EnumSource(NotificationType.class)
  @DisplayName("성공: NotificationType별 level을 매핑한다")
  void fromType_mapsLevel(NotificationType type) {
    NotificationLevel level = NotificationLevel.fromType(type);

    if (type == NotificationType.ROLE_CHANGED) {
      assertThat(level).isEqualTo(NotificationLevel.WARNING);
    } else {
      assertThat(level).isEqualTo(NotificationLevel.INFO);
    }
  }

  @Test
  @DisplayName("성공: CursorResponseNotificationDto를 생성한다")
  void cursorResponse_from() {
    NotificationDto dto = new NotificationDto(
        UUID.randomUUID(),
        Instant.now(),
        UUID.randomUUID(),
        "title",
        "description",
        NotificationLevel.INFO
    );
    var page = new com.mopl.global.response.CursorPageResponse<>(
        java.util.List.of(dto),
        "2026-01-01T00:00:00Z",
        UUID.randomUUID(),
        true,
        10L,
        "createdAt",
        "ASCENDING"
    );

    CursorResponseNotificationDto response = CursorResponseNotificationDto.from(page);

    assertThat(response.data()).hasSize(1);
    assertThat(response.hasNext()).isTrue();
    assertThat(response.totalCount()).isEqualTo(10L);
    assertThat(response.sortBy()).isEqualTo("createdAt");
    assertThat(response.sortDirection()).isEqualTo("ASCENDING");
  }
}
