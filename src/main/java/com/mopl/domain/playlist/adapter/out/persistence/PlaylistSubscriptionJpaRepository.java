package com.mopl.domain.playlist.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistSubscriptionJpaRepository extends JpaRepository<PlaylistSubscriptionJpaEntity, UUID> {

  boolean existsByPlaylistIdAndSubscriberId(UUID playlistId, UUID subscriberId);

  long countByPlaylistId(UUID playlistId);

  List<PlaylistSubscriptionJpaEntity> findByPlaylistId(UUID playlistId);

  Optional<PlaylistSubscriptionJpaEntity> findByPlaylistIdAndSubscriberId(UUID playlistId, UUID subscriberId);

  void deleteByPlaylistIdAndSubscriberId(UUID playlistId, UUID subscriberId);

  void deleteByPlaylistId(UUID playlistId);

  @Query("SELECT s.playlist.id as playlistId, COUNT(s) as count " +
         "FROM PlaylistSubscriptionJpaEntity s " +
         "WHERE s.playlist.id IN :playlistIds " +
         "GROUP BY s.playlist.id")
  List<PlaylistSubscriptionCount> countByPlaylistIds(@Param("playlistIds") List<UUID> playlistIds);

  @Query("SELECT s.playlist.id " +
         "FROM PlaylistSubscriptionJpaEntity s " +
         "WHERE s.playlist.id IN :playlistIds AND s.subscriberId = :subscriberId")
  List<UUID> findSubscribedPlaylistIds(@Param("playlistIds") List<UUID> playlistIds, @Param("subscriberId") UUID subscriberId);
}
