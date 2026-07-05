package com.mopl.domain.playlist.adapter.in.web;

import com.mopl.domain.playlist.application.dto.PlaylistCreateRequest;
import com.mopl.domain.playlist.application.dto.PlaylistDto;
import com.mopl.domain.playlist.application.dto.PlaylistSearchRequest;
import com.mopl.domain.playlist.application.dto.PlaylistUpdateRequest;
import com.mopl.domain.playlist.application.dto.SortBy;
import com.mopl.domain.playlist.application.port.in.PlaylistUseCase;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

  private final PlaylistUseCase playlistUseCase;

  @GetMapping
  public ResponseEntity<CursorPageResponse<PlaylistDto>> getPlaylists(
      @AuthenticationPrincipal JwtClaims claims,
      @RequestParam(required = false) String keywordLike,
      @RequestParam(required = false) UUID ownerIdEqual,
      @RequestParam(required = false) UUID subscriberIdEqual,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam(defaultValue = "updatedAt") SortBy sortBy,
      @RequestParam(defaultValue = "DESCENDING") SortDirection sortDirection) {

    PlaylistSearchRequest request = new PlaylistSearchRequest(
        keywordLike,
        ownerIdEqual,
        subscriberIdEqual,
        cursor,
        idAfter,
        limit,
        sortBy,
        sortDirection
    );

    return ResponseEntity.ok(
        playlistUseCase.getList(claims.getUserId(), request)
    );
  }

  @PostMapping
  public ResponseEntity<PlaylistDto> createPlaylist(
      @AuthenticationPrincipal JwtClaims claims,
      @Valid @RequestBody PlaylistCreateRequest request) {

    PlaylistDto response = playlistUseCase.create(claims.getUserId(), request);
    return ResponseEntity.status(201).body(response);
  }

  @GetMapping("/{playlistId}")
  public ResponseEntity<PlaylistDto> getPlaylist(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId) {

    return ResponseEntity.ok(
        playlistUseCase.getById(playlistId, claims.getUserId())
    );
  }

  @PatchMapping("/{playlistId}")
  public ResponseEntity<PlaylistDto> updatePlaylist(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId,
      @Valid @RequestBody PlaylistUpdateRequest request) {

    return ResponseEntity.ok(
        playlistUseCase.update(playlistId, claims.getUserId(), request)
    );
  }

  @DeleteMapping("/{playlistId}")
  public ResponseEntity<Void> deletePlaylist(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId) {

    playlistUseCase.delete(playlistId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> subscribe(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId) {

    playlistUseCase.subscribe(playlistId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{playlistId}/subscription")
  public ResponseEntity<Void> unsubscribe(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId) {

    playlistUseCase.unsubscribe(playlistId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> addContent(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId) {

    playlistUseCase.addContent(playlistId, contentId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{playlistId}/contents/{contentId}")
  public ResponseEntity<Void> removeContent(
      @AuthenticationPrincipal JwtClaims claims,
      @PathVariable UUID playlistId,
      @PathVariable UUID contentId) {

    playlistUseCase.removeContent(playlistId, contentId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }
}