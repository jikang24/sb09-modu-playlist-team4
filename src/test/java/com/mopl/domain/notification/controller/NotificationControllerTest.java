package com.mopl.domain.notification.controller;

import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.dto.NotificationLevel;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.GlobalExceptionHandler;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController 테스트")
class NotificationControllerTest {

  private MockMvc mockMvc;

  @Mock
  private NotificationService notificationService;

  private UUID userId;
  private NotificationDto notificationDto;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new NotificationController(notificationService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

    userId = UUID.randomUUID();
    notificationDto = new NotificationDto(
        UUID.randomUUID(),
        Instant.parse("2026-01-01T00:00:00Z"),
        userId,
        "새 팔로워",
        "누군가 팔로우했습니다.",
        NotificationLevel.INFO
    );
  }

  @Nested
  @DisplayName("GET /api/notifications")
  class GetNotifications {

    @Test
    @DisplayName("성공: 알림 목록을 조회한다")
    void getNotifications_success() throws Exception {
      CursorPageResponse<NotificationDto> response = new CursorPageResponse<>(
          List.of(notificationDto),
          null,
          null,
          false,
          1L,
          "createdAt",
          "ASCENDING"
      );
      given(notificationService.findMyNotifications(any())).willReturn(response);

      mockMvc.perform(get("/api/notifications")
              .param("limit", "10")
              .param("sortDirection", "ASCENDING")
              .param("sortBy", "createdAt"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data", hasSize(1)))
          .andExpect(jsonPath("$.data[0].title").value("새 팔로워"))
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("실패: 잘못된 커서면 400 Bad Request")
    void getNotifications_invalidCursor() throws Exception {
      given(notificationService.findMyNotifications(any()))
          .willThrow(new MoplException(ErrorCode.INVALID_NOTIFICATION_CURSOR));

      mockMvc.perform(get("/api/notifications")
              .param("limit", "10")
              .param("sortDirection", "ASCENDING")
              .param("sortBy", "createdAt")
              .param("cursor", "bad-cursor")
              .param("idAfter", UUID.randomUUID().toString()))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("DELETE /api/notifications/{notificationId}")
  class ReadNotification {

    @Test
    @DisplayName("성공: 알림 읽음 처리 시 204 No Content")
    void read_success() throws Exception {
      UUID notificationId = UUID.randomUUID();

      mockMvc.perform(delete("/api/notifications/" + notificationId))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("실패: 알림이 없으면 404 Not Found")
    void read_notFound() throws Exception {
      UUID notificationId = UUID.randomUUID();
      doThrow(new MoplException(ErrorCode.NOTIFICATION_NOT_FOUND))
          .when(notificationService).read(notificationId);

      mockMvc.perform(delete("/api/notifications/" + notificationId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("실패: 권한이 없으면 403 Forbidden")
    void read_accessDenied() throws Exception {
      UUID notificationId = UUID.randomUUID();
      doThrow(new MoplException(ErrorCode.NOTIFICATION_ACCESS_DENIED))
          .when(notificationService).read(notificationId);

      mockMvc.perform(delete("/api/notifications/" + notificationId))
          .andExpect(status().isForbidden());
    }
  }
}
