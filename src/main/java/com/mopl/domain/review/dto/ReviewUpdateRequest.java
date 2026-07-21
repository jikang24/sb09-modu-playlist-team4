package com.mopl.domain.review.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record ReviewUpdateRequest(
    String text,
    @DecimalMin(value = "0.5") @DecimalMax(value = "5.0") double rating
) {

}
