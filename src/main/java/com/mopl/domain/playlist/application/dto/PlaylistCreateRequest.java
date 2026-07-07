package com.mopl.domain.playlist.application.dto;

import jakarta.validation.constraints.NotBlank;

public record PlaylistCreateRequest(
    @NotBlank String title,
    String description
) {
}
