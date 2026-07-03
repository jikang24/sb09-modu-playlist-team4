package com.mopl.global.event;

import java.util.UUID;

public record NotificationRequestedEvent(
    UUID receiverId,
    String type,
    String title,
    String content
) {
}
