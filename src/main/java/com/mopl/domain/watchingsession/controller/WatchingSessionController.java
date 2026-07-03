package com.mopl.domain.watchingsession.controller;

import com.mopl.domain.watchingsession.dto.WatchingSessionDto;
import com.mopl.domain.watchingsession.dto.WatchingSessionSearchRequest;
import com.mopl.domain.watchingsession.service.WatchingSessionService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * [Adapter In] 시청 세션(같이 보기) 컨트롤러
 *
 * 리소스가 /api/users, /api/contents 두 갈래로 나뉘어 있어 클래스 레벨 @RequestMapping 없이
 * 메서드마다 전체 경로를 명시함
 */
@RestController
@RequiredArgsConstructor
public class WatchingSessionController {

  private final WatchingSessionService watchingSessionService;

  /** 특정 사용자가 지금 보고 있는 세션 조회 (안 보고 있으면 null) */
  @GetMapping("/api/users/{watcherId}/watching-sessions")
  public ResponseEntity<WatchingSessionDto> getByWatcher(@PathVariable UUID watcherId) {
    return ResponseEntity.ok(watchingSessionService.getByWatcherId(watcherId));
  }

  /** 시청 퇴장 - 본인 세션만 종료 가능 */
  @DeleteMapping("/api/users/{watcherId}/watching-sessions")
  public ResponseEntity<Void> leave(
      @PathVariable UUID watcherId,
      @AuthenticationPrincipal JwtClaims claims) {
    validateSelf(watcherId, claims);
    watchingSessionService.leave(watcherId);
    return ResponseEntity.noContent().build();
  }

  /** 특정 콘텐츠를 보고 있는 세션 목록 (커서 페이지네이션) */
  @GetMapping("/api/contents/{contentId}/watching-sessions")
  public ResponseEntity<CursorPageResponse<WatchingSessionDto>> getByContent(
      @PathVariable UUID contentId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESCENDING") String sortDirection) {

    WatchingSessionSearchRequest request = new WatchingSessionSearchRequest(
        contentId, cursor, idAfter, limit, sortBy, sortDirection);
    return ResponseEntity.ok(watchingSessionService.getByContentId(request));
  }

  /** 시청 입장 - 요청자 본인이 watcher가 됨 */
  @PostMapping("/api/contents/{contentId}/watching-sessions")
  public ResponseEntity<WatchingSessionDto> enter(
      @PathVariable UUID contentId,
      @AuthenticationPrincipal JwtClaims claims) {
    WatchingSessionDto dto = watchingSessionService.enter(claims.getUserId(), contentId);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  private void validateSelf(UUID watcherId, JwtClaims claims) {
    if (!watcherId.equals(claims.getUserId())) {
      throw new MoplException(ErrorCode.FORBIDDEN);
    }
  }
}