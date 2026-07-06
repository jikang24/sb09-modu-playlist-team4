package com.mopl.domain.playlist.adapter.in.web;

import com.mopl.domain.playlist.application.dto.PlaylistCreateRequest;
import com.mopl.domain.playlist.application.dto.PlaylistDto;
import com.mopl.domain.playlist.application.dto.PlaylistSearchRequest;
import com.mopl.domain.playlist.application.dto.PlaylistUpdateRequest;
import com.mopl.domain.playlist.application.port.in.PlaylistUseCase;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

  private final PlaylistUseCase playlistUseCase;

  @GetMapping
  public ResponseEntity<CursorPageResponse<PlaylistDto>> getPlaylists(
      @AuthenticationPrincipal JwtClaims claims,
      PlaylistSearchRequest request
  ) {
    log.info("[Playlist] 목록 조회 요청 - userId: {}", claims.getUserId());

    return ResponseEntity.ok(
        playlistUseCase.getList(claims.getUserId(), request)
    );
  }

  @PostMapping
  public ResponseEntity<PlaylistDto> createPlaylist(
      @AuthenticationPrincipal JwtClaims claims,
      @Valid @RequestBody PlaylistCreateRequest request) {

    log.info("[Playlist] 생성 요청 - userId: {}", claims.getUserId());

    PlaylistDto response = playlistUseCase.create(claims.getUserId(), request);
    return ResponseEntity.status(201).body(response);
  }

  @GetMapping("/{playlistId}")
  public ResponseEntity<PlaylistDto> getPlaylist(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId) {

    log.info("[Playlist] 단건 조회 요청 - playlistId: {}, userId: {}",
        playlistId, claims.getUserId());

    return ResponseEntity.ok(
        playlistUseCase.getById(playlistId, claims.getUserId())
    );
  }

  @PatchMapping("/{playlistId}")
  public ResponseEntity<PlaylistDto> updatePlaylist(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId,
      @Valid @RequestBody PlaylistUpdateRequest request) {

    log.info("[Playlist] 수정 요청 - playlistId: {}, userId: {}",
        playlistId, claims.getUserId());

    return ResponseEntity.ok(
        playlistUseCase.update(playlistId, claims.getUserId(), request)
    );
  }

  @DeleteMapping("/{playlistId}")
  public ResponseEntity<Void> deletePlaylist(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId) {

    log.info("[Playlist] 삭제 요청 - playlistId: {}, userId: {}",
        playlistId, claims.getUserId());

    playlistUseCase.delete(playlistId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> subscribe(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId) {

    log.info("[Playlist] 구독 요청 - playlistId: {}, userId: {}",
        playlistId, claims.getUserId());

    playlistUseCase.subscribe(playlistId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> unsubscribe(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId) {

    log.info("[Playlist] 구독 취소 요청 - playlistId: {}, userId: {}",
        playlistId, claims.getUserId());

    playlistUseCase.unsubscribe(playlistId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> addContent(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId) {

    log.info("[Playlist] 콘텐츠 추가 요청 - playlistId: {}, contentId: {}, userId: {}",
        playlistId, contentId, claims.getUserId());

    playlistUseCase.addContent(playlistId, contentId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> removeContent(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId) {

    log.info("[Playlist] 콘텐츠 삭제 요청 - playlistId: {}, contentId: {}, userId: {}",
        playlistId, contentId, claims.getUserId());

    playlistUseCase.removeContent(playlistId, contentId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }
}