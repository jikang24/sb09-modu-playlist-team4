package com.mopl.domain.playlist.adapter.out.persistence;

import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import java.util.List;

public interface PlaylistRepositoryCustom {

  List<PlaylistJpaEntity> findAllWithCursor(PlaylistSearchCondition condition);

  long countAll(PlaylistSearchCondition condition);
}