package com.mopl.global.event;

import java.util.UUID;

public record WatchingSessionStartedEvent(
    UUID watcherId,
    UUID contentId,
    String contentTitle
) {
}
