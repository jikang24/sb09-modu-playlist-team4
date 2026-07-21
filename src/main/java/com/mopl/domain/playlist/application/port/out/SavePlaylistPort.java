package com.mopl.domain.playlist.application.port.out;

import com.mopl.domain.playlist.domain.Playlist;
import java.util.UUID;

public interface SavePlaylistPort {

  Playlist save(Playlist playlist);

  void delete(UUID playlistId);

  void subscribe(UUID playlistId, UUID subscriberId);

  void unsubscribe(UUID playlistId, UUID subscriberId);

  /** 콘텐츠가 삭제됐을 때, 이 콘텐츠를 담고 있는 모든 플레이리스트에서 일괄 제거 */
  void removeContentFromAllPlaylists(UUID contentId);
}
