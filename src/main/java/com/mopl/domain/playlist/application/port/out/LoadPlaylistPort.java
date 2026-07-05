package com.mopl.domain.playlist.application.port.out;

import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import com.mopl.domain.playlist.domain.Playlist;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoadPlaylistPort {

  Optional<Playlist> findById(UUID id);

  List<Playlist> findAllByCondition(PlaylistSearchCondition condition);

  long countByCondition(PlaylistSearchCondition condition);

  long countSubscribers(UUID playlistId);

  boolean isSubscribed(UUID playlistId, UUID subscriberId);

  List<UUID> findSubscriberIds(UUID playlistId);
}
