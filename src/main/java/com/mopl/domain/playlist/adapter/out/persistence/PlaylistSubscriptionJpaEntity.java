package com.mopl.domain.playlist.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "playlist_subscriptions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "subscriber_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistSubscriptionJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "playlist_id", nullable = false)
  private PlaylistJpaEntity playlist;

  @Column(name = "subscriber_id", nullable = false, columnDefinition = "uuid")
  private UUID subscriberId;

  private PlaylistSubscriptionJpaEntity(UUID subscriberId) {
    this.subscriberId = subscriberId;
  }

  public static PlaylistSubscriptionJpaEntity of(UUID subscriberId) {
    return new PlaylistSubscriptionJpaEntity(subscriberId);
  }

  public void assignPlaylist(PlaylistJpaEntity playlist) {
    this.playlist = playlist;
  }
}
