package com.mopl.domain.conversation.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Conversation {
  private final UUID id;
  private final List<UUID> participants;
  private final LocalDateTime createdAt;

  public Conversation(UUID id, List<UUID> participants, LocalDateTime createdAt) {
    this.id = id;
    this.participants = participants;
    this.createdAt = createdAt;
  }

  public static Conversation create(UUID myId,UUID otherId) {
    if(myId.equals(otherId)){
      throw new MoplException(ErrorCode.CANNOT_TALK_TO_SELF);
    }
    return new Conversation(UUID.randomUUID(),List.of(myId,otherId),LocalDateTime.now());
  }

  public UUID getOtherParticipant(UUID participantId) {
    return participants.stream()
        .filter(id -> !id.equals(participantId))
        .findFirst()
        .orElseThrow(()->new MoplException(ErrorCode.PARTICIPANTS_NOT_FOUND));
  }

  public boolean hasParticipant(UUID participantId) {
    return participants.contains(participantId);
  }

  public UUID getId() {
    return id;
  }
  public List<UUID> getParticipants() {
    return participants;
  }
  public LocalDateTime getCreatedAt() {
    return createdAt;
  }



}
