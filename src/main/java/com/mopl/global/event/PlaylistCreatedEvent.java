package com.mopl.global.event;

import java.util.UUID;

public record PlaylistCreatedEvent(
    UUID playlistId,
    UUID ownerId,
    String title,
    String description
) {
}
