package com.mopl.global.event;

import java.util.UUID;

public record TempPasswordIssuedEvent(
        UUID userId,
        String email,
        String tempPassword
) {
}
