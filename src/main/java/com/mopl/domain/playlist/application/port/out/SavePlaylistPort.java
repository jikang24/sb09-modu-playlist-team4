package com.mopl.domain.playlist.application.port.out;

import com.mopl.domain.playlist.domain.Playlist;
import java.util.UUID;

public interface SavePlaylistPort {

  Playlist save(Playlist playlist);

  void delete(UUID playlistId);

  void subscribe(UUID playlistId, UUID subscriberId);

  void unsubscribe(UUID playlistId, UUID subscriberId);
}
