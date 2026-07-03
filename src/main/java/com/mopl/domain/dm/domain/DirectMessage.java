package com.mopl.domain.dm.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.time.Instant;
import java.util.UUID;

public class DirectMessage {

  private final UUID id;
  private final UUID conversationId;
  private final UUID senderId;
  private final UUID receiverId;
  private final String content;
  private final Instant createdAt;
  private boolean read;

  public DirectMessage(UUID id, UUID conversationId, UUID senderId, UUID receiverId,
      String content, Instant createdAt, boolean read) {
    this.id = id;
    this.conversationId = conversationId;
    this.senderId = senderId;
    this.receiverId = receiverId;
    this.content = content;
    this.createdAt = createdAt;
    this.read = read;
  }

  public static DirectMessage create(UUID conversationId, UUID senderId, UUID receiverId, String content) {
    if (content == null || content.isBlank()) {
      throw new MoplException(ErrorCode.INVALID_INPUT);
    }
    return new DirectMessage(
        UUID.randomUUID(), conversationId, senderId, receiverId,
        content, Instant.now(), false
    );
  }

  public void markAsRead() {
    this.read = true;
  }

  public boolean isSender(UUID userId) {
    return senderId.equals(userId);
  }

  public UUID getId() { return id; }
  public UUID getConversationId() { return conversationId; }
  public UUID getSenderId() { return senderId; }
  public UUID getReceiverId() { return receiverId; }
  public String getContent() { return content; }
  public Instant getCreatedAt() { return createdAt; }
  public boolean isRead() { return read; }
}