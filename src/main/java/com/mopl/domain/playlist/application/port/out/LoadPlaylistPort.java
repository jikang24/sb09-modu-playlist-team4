package com.mopl.domain.playlist.application.port.out;

import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import com.mopl.domain.playlist.domain.Playlist;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface LoadPlaylistPort {

  Optional<Playlist> findById(UUID id);

  /**
   * 콘텐츠 추가/삭제, 메타데이터 수정처럼 조회 후 그대로 다시 저장하는 흐름 전용 조회.
   * 행 잠금을 걸어 동시 수정으로 인한 lost update(SavePlaylistPort.save 참고)를 막는다.
   * 캐시를 거치지 않고 항상 DB에서 최신 상태를 잠근 채로 읽는다.
   */
  Optional<Playlist> findByIdForUpdate(UUID id);

  List<Playlist> findAllByCondition(PlaylistSearchCondition condition);

  long countByCondition(PlaylistSearchCondition condition);

  long countSubscribers(UUID playlistId);

  boolean isSubscribed(UUID playlistId, UUID subscriberId);

  List<UUID> findSubscriberIds(UUID playlistId);

  Map<UUID, Long> countSubscribersBulk(List<UUID> playlistIds);

  Map<UUID, Boolean> isSubscribedBulk(List<UUID> playlistIds, UUID subscriberId);
}
