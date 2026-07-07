package com.mopl.domain.playlist.adapter.out.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "playlists")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaylistJpaEntity {

  @Id
  @Column(columnDefinition = "uuid", nullable = false, updatable = false)
  private UUID id;

  @Column(name = "owner_id", nullable = false, columnDefinition = "uuid")
  private UUID ownerId;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "created_at", nullable = false, columnDefinition = "timestamp with time zone")
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
  private Instant updatedAt;

  @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("position ASC")
  private List<PlaylistContentJpaEntity> contents = new ArrayList<>();

  @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PlaylistSubscriptionJpaEntity> subscriptions = new ArrayList<>();

  private PlaylistJpaEntity(UUID id, UUID ownerId, String title, String description,
      Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.ownerId = ownerId;
    this.title = title;
    this.description = description;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static PlaylistJpaEntity of(UUID id, UUID ownerId, String title, String description,
      Instant createdAt, Instant updatedAt) {
    return new PlaylistJpaEntity(id, ownerId, title, description, createdAt, updatedAt);
  }

  void updateContents(List<PlaylistContentJpaEntity> newContents) {
    this.contents.clear();
    this.contents.addAll(newContents);
    newContents.forEach(content -> content.assignPlaylist(this));
  }

  void updateMetadata(String title, String description, Instant updatedAt) {
    this.title = title;
    this.description = description;
    this.updatedAt = updatedAt;
  }
}
