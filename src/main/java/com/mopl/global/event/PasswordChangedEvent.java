package com.mopl.global.event;

import java.util.UUID;

public record PasswordChangedEvent(
        UUID userId
) {
}
