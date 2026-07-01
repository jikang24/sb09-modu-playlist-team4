package com.mopl.domain.conversation.domain;

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

  public UUID getOtherParticipant(UUID participantId) {
    return participants.stream()
        .filter(id -> !id.equals(participantId))
        .findFirst()
        .orElseThrow();
  }
  public boolean isParticipant(UUID participantId) {
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
