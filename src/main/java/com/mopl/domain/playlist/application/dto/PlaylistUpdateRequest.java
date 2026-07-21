package com.mopl.domain.playlist.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaylistUpdateRequest(
    @NotBlank String title,
    String description
) {
}
