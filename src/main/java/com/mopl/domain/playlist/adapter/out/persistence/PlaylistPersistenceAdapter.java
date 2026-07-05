package com.mopl.domain.playlist.adapter.out.persistence;

import com.mopl.domain.playlist.application.dto.PlaylistSearchCondition;
import com.mopl.domain.playlist.application.port.out.LoadPlaylistPort;
import com.mopl.domain.playlist.application.port.out.SavePlaylistPort;
import com.mopl.domain.playlist.domain.Playlist;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaylistPersistenceAdapter implements SavePlaylistPort, LoadPlaylistPort {

  private final PlaylistJpaRepository playlistJpaRepository;
  private final PlaylistSubscriptionJpaRepository subscriptionJpaRepository;
  private final PlaylistPersistenceMapper mapper;

  @Override
  public Playlist save(Playlist playlist) {
    PlaylistJpaEntity existing = playlistJpaRepository.findById(playlist.getId()).orElse(null);
    PlaylistJpaEntity entity = mapper.toJpaEntity(playlist, existing);
    PlaylistJpaEntity saved = playlistJpaRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public void delete(UUID playlistId) {
    subscriptionJpaRepository.deleteByPlaylistId(playlistId);
    playlistJpaRepository.deleteById(playlistId);
  }

  @Override
  public void subscribe(UUID playlistId, UUID subscriberId) {
    PlaylistJpaEntity playlist = playlistJpaRepository.findById(playlistId)
        .orElseThrow(() -> new MoplException(ErrorCode.PLAYLIST_NOT_FOUND));

    PlaylistSubscriptionJpaEntity subscription = PlaylistSubscriptionJpaEntity.of(subscriberId);
    subscription.assignPlaylist(playlist);

    subscriptionJpaRepository.saveAndFlush(subscription);
  }

  @Override
  @org.springframework.transaction.annotation.Transactional
  public void unsubscribe(UUID playlistId, UUID subscriberId) {
    subscriptionJpaRepository.deleteByPlaylistIdAndSubscriberId(playlistId, subscriberId);
  }

  @Override
  public Optional<Playlist> findById(UUID id) {
    return playlistJpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<Playlist> findAllByCondition(PlaylistSearchCondition condition) {

    return playlistJpaRepository.findAllWithCursor(condition)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public long countByCondition(PlaylistSearchCondition condition) {
    return playlistJpaRepository.countAll(condition);
  }

  @Override
  public long countSubscribers(UUID playlistId) {
    return subscriptionJpaRepository.countByPlaylistId(playlistId);
  }

  @Override
  public boolean isSubscribed(UUID playlistId, UUID subscriberId) {
    return subscriptionJpaRepository.existsByPlaylistIdAndSubscriberId(playlistId, subscriberId);
  }

  @Override
  public List<UUID> findSubscriberIds(UUID playlistId) {
    return subscriptionJpaRepository.findByPlaylistId(playlistId).stream()
        .map(PlaylistSubscriptionJpaEntity::getSubscriberId)
        .toList();
  }
}
