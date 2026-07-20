package com.mopl.domain.playlist.adapter.out.persistence;

import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import java.util.List;
import java.util.UUID;

public interface PlaylistRepositoryCustom {

  List<PlaylistJpaEntity> findAllWithCursor(PlaylistSearchCondition condition);

  long countAll(PlaylistSearchCondition condition);

  /** playlistIds에 속한 PlaylistContent를 한 번에 조회 (N+1 방지, position 오름차순) */
  List<PlaylistContentJpaEntity> findContentsByPlaylistIds(List<UUID> playlistIds);
}