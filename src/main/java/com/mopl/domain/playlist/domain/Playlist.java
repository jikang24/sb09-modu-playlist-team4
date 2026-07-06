package com.mopl.domain.playlist.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Playlist {

  private final UUID id;
  private final UUID ownerId;
  private String title;
  private String description;
  private final Instant createdAt;
  private Instant updatedAt;
  private final List<UUID> contentIds;

  private Playlist(UUID id, UUID ownerId, String title, String description,
      Instant createdAt, Instant updatedAt, List<UUID> contentIds) {
    this.id = id;
    this.ownerId = ownerId;
    this.title = title;
    this.description = description;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.contentIds = contentIds != null ? new ArrayList<>(contentIds) : new ArrayList<>();
  }

  public static Playlist create(UUID ownerId, String title, String description) {
    validateTitle(title);
    Instant now = Instant.now();
    return new Playlist(UUID.randomUUID(), ownerId, title, description, now, now, List.of());
  }

  public static Playlist restore(UUID id, UUID ownerId, String title, String description,
      Instant createdAt, Instant updatedAt, List<UUID> contentIds) {
    return new Playlist(id, ownerId, title, description, createdAt, updatedAt, contentIds);
  }

  public void update(String title, String description) {
    validateTitle(title);
    this.title = title;
    this.description = description;
    this.updatedAt = Instant.now();
  }

  public void addContent(UUID contentId) {
    if (contentIds.contains(contentId)) {
      throw new MoplException(ErrorCode.PLAYLIST_CONTENT_ALREADY_EXISTS);
    }
    contentIds.add(contentId);
    this.updatedAt = Instant.now();
  }

  public void removeContent(UUID contentId) {
    if (!contentIds.remove(contentId)) {
      throw new MoplException(ErrorCode.PLAYLIST_CONTENT_NOT_FOUND);
    }
    this.updatedAt = Instant.now();
  }

  public boolean isOwner(UUID userId) {
    return ownerId.equals(userId);
  }

  private static void validateTitle(String title) {
    if (title == null || title.isBlank()) {
      throw new MoplException(ErrorCode.INVALID_INPUT);
    }
  }

  public UUID getId() { return id; }
  public UUID getOwnerId() { return ownerId; }
  public String getTitle() { return title; }
  public String getDescription() { return description; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public List<UUID> getContentIds() { return List.copyOf(contentIds); }
}
