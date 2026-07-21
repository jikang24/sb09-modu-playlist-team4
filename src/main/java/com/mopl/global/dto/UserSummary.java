package com.mopl.global.dto;

import java.util.UUID;

public record UserSummary (
        UUID userId,
        String name,
        String profileImageUrl
){
}
