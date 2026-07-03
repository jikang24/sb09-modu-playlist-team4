package com.mopl.domain.conversation.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Conversation {

  private final UUID id;
  private final UUID participant1Id;
  private final UUID participant2Id;
  private final Instant createdAt;

  public Conversation(UUID id, UUID participant1Id, UUID participant2Id, Instant createdAt) {
    this.id = id;
    this.participant1Id = participant1Id;
    this.participant2Id = participant2Id;
    this.createdAt = createdAt;
  }

  public static Conversation create(UUID myId, UUID otherId) {
    if (myId.equals(otherId)) {
      throw new MoplException(ErrorCode.CANNOT_TALK_TO_SELF);
    }
    return new Conversation(UUID.randomUUID(), myId, otherId, Instant.now());
  }

  public UUID getOtherParticipant(UUID participantId) {
    if (participant1Id.equals(participantId)) {
      return participant2Id;
    }
    if (participant2Id.equals(participantId)) {
      return participant1Id;
    }
    throw new MoplException(ErrorCode.PARTICIPANTS_NOT_FOUND);
  }

  public boolean hasParticipant(UUID participantId) {
    return participant1Id.equals(participantId) || participant2Id.equals(participantId);
  }

  public List<UUID> getParticipants() {
    return List.of(participant1Id, participant2Id);
  }

  public UUID getId() { return id; }
  public UUID getParticipant1Id() { return participant1Id; }
  public UUID getParticipant2Id() { return participant2Id; }
  public Instant getCreatedAt() { return createdAt; }
}