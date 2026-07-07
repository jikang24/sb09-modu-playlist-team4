package com.mopl.domain.playlist.application.port.in;

import com.mopl.domain.playlist.application.dto.PlaylistCreateRequest;
import com.mopl.domain.playlist.application.dto.PlaylistDto;
import com.mopl.domain.playlist.application.dto.PlaylistSearchRequest;
import com.mopl.domain.playlist.application.dto.PlaylistUpdateRequest;
import com.mopl.global.response.CursorPageResponse;
import java.util.UUID;

public interface PlaylistUseCase {

  PlaylistDto create(UUID ownerId, PlaylistCreateRequest request);

  PlaylistDto getById(UUID playlistId, UUID currentUserId);

  CursorPageResponse<PlaylistDto> getList(UUID currentUserId, PlaylistSearchRequest request);

  PlaylistDto update(UUID playlistId, UUID currentUserId, PlaylistUpdateRequest request);

  void delete(UUID playlistId, UUID currentUserId);

  void subscribe(UUID playlistId, UUID subscriberId);

  void unsubscribe(UUID playlistId, UUID subscriberId);

  void addContent(UUID playlistId, UUID contentId, UUID currentUserId);

  void removeContent(UUID playlistId, UUID contentId, UUID currentUserId);
}
