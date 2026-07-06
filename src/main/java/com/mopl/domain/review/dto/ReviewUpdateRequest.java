package com.mopl.domain.review.dto;

public record ReviewUpdateRequest(
    String text,
    double rating
) {

}
