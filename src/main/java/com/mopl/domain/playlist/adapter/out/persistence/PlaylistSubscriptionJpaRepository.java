package com.mopl.domain.playlist.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistSubscriptionJpaRepository extends JpaRepository<PlaylistSubscriptionJpaEntity, UUID> {

  boolean existsByPlaylistIdAndSubscriberId(UUID playlistId, UUID subscriberId);

  long countByPlaylistId(UUID playlistId);

  List<PlaylistSubscriptionJpaEntity> findByPlaylistId(UUID playlistId);

  Optional<PlaylistSubscriptionJpaEntity> findByPlaylistIdAndSubscriberId(UUID playlistId, UUID subscriberId);

  void deleteByPlaylistIdAndSubscriberId(UUID playlistId, UUID subscriberId);

  void deleteByPlaylistId(UUID playlistId);
}
