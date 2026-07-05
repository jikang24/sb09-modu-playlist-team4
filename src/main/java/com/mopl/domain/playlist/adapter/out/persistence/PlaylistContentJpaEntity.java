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
    name = "playlist_contents",
    uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "content_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistContentJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(columnDefinition = "uuid", updatable = false, nullable = false)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "playlist_id", nullable = false)
  private PlaylistJpaEntity playlist;

  @Column(name = "content_id", nullable = false, columnDefinition = "uuid")
  private UUID contentId;

  @Column(nullable = false)
  private int position;

  private PlaylistContentJpaEntity(UUID contentId, int position) {
    this.contentId = contentId;
    this.position = position;
  }

  public static PlaylistContentJpaEntity of(UUID contentId, int position) {
    return new PlaylistContentJpaEntity(contentId, position);
  }

  void assignPlaylist(PlaylistJpaEntity playlist) {
    this.playlist = playlist;
  }
}
