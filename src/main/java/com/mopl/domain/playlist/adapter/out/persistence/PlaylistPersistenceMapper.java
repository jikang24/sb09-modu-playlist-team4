package com.mopl.domain.playlist.adapter.out.persistence;

import com.mopl.domain.playlist.domain.Playlist;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlaylistPersistenceMapper {

  public Playlist toDomain(PlaylistJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return toDomain(entity, entity.getContents());
  }

  /**
   * 목록 조회 등 여러 Playlist를 한 번에 변환할 때 사용.
   * entity.getContents()(지연 로딩)를 건마다 트리거하지 않도록, 호출부에서 벌크 조회해 둔
   * contents를 그대로 전달받는다 (N+1 방지).
   */
  public Playlist toDomain(PlaylistJpaEntity entity, List<PlaylistContentJpaEntity> contents) {
    if (entity == null) {
      return null;
    }
    List<UUID> contentIds = contents.stream()
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
