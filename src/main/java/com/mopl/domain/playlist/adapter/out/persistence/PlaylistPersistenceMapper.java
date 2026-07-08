package com.mopl.domain.playlist.adapter.out.persistence;

import com.mopl.domain.playlist.domain.Playlist;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlaylistPersistenceMapper {

  public Playlist toDomain(PlaylistJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    List<UUID> contentIds = entity.getContents().stream()
        .map(PlaylistContentJpaEntity::getContentId)
        .toList();
    return Playlist.restore(
        entity.getId(),
        entity.getOwnerId(),
        entity.getTitle(),
        entity.getDescription(),
        entity.getCreatedAt(),
        entity.getUpdatedAt(),
        contentIds
    );
  }

  public PlaylistJpaEntity toJpaEntity(Playlist playlist, PlaylistJpaEntity existing) {
    PlaylistJpaEntity entity = existing != null
        ? existing
        : PlaylistJpaEntity.of(
            playlist.getId(),
            playlist.getOwnerId(),
            playlist.getTitle(),
            playlist.getDescription(),
            playlist.getCreatedAt(),
            playlist.getUpdatedAt()
        );

    if (existing != null) {
      entity.updateMetadata(playlist.getTitle(), playlist.getDescription(), playlist.getUpdatedAt());
    }
    return entity;
  }
}
