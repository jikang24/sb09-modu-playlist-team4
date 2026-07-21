package com.mopl.domain.dm.adapter.out.persistence;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "direct_message")
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class DirectMessageJpaEntity {
  @Id
  private UUID id;

  @Column(name = "sender_id", nullable = false)
  private UUID senderId;
  @Column(name = "receiver_id", nullable = false)
  private UUID receiverId;
  @Column(name = "conversation_id", nullable = false)
  private UUID conversationId;
  @Column(name = "content", nullable = false)
  private String content;
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
  @Column(name = "read", nullable = false)
  private boolean read;

  @Builder
  private DirectMessageJpaEntity(UUID id, UUID conversationId, UUID senderId,
      UUID receiverId, String content, Instant createdAt, boolean read) {
    this.id = id;
    this.conversationId = conversationId;
    this.senderId = senderId;
    this.receiverId = receiverId;
    this.content = content;
    this.createdAt = createdAt;
    this.read = read;
  }

}
