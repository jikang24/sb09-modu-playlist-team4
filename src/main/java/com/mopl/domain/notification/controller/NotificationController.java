package com.mopl.domain.notification.controller;

import com.mopl.domain.notification.dto.CursorResponseNotificationDto;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.dto.NotificationSearchRequest;
import com.mopl.domain.notification.dto.NotificationSortBy;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.global.dto.SortDirection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "알림")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @Operation(summary = "알림 목록 조회 (커서 페이지네이션)", description = "API 요청자의 알림 목록만 조회할 수 있습니다.")
  @GetMapping
  public ResponseEntity<CursorResponseNotificationDto> getNotifications(
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam SortDirection sortDirection,
      @RequestParam NotificationSortBy sortBy
  ) {
    NotificationSearchRequest request =
        new NotificationSearchRequest(cursor, idAfter, limit, sortDirection, sortBy);
    return ResponseEntity.ok(
        CursorResponseNotificationDto.from(notificationService.findMyNotifications(request))
    );
  }

  @Operation(summary = "알림 읽음 처리")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "403", description = "권한 오류"),
      @ApiResponse(responseCode = "404", description = "해당 리소스 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @DeleteMapping("/{notificationId}")
  public ResponseEntity<Void> read(@PathVariable UUID notificationId) {
    notificationService.read(notificationId);
    return ResponseEntity.noContent().build();
  }
}
