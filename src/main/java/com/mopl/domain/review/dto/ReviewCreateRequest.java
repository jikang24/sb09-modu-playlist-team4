package com.mopl.domain.review.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.util.UUID;

public record ReviewCreateRequest(
    UUID contentId,
    String text,
    @DecimalMin(value = "0.5") @DecimalMax(value = "5.0") double rating
) {

}
