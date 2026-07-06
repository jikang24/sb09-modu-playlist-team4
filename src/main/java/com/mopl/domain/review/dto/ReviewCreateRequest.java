package com.mopl.domain.review.dto;

import java.util.UUID;

public record ReviewCreateRequest(
    UUID contentId,
    String text,
    double rating
) {

}
