package com.mopl.domain.review.dto;

import java.math.BigDecimal;
import java.util.UUID;

// GET /api/reviews 응답에 쓰이는 리뷰 DTO
public record ReviewDto(
    UUID id,
    UUID contentId,
    AuthorDto author,
    String text,
    BigDecimal rating
) {
  // 리뷰 작성자 정보 - User 모듈에서 가져온 값을 여기 담음
  public record AuthorDto(UUID userId, String name, String profileImageUrl) {}
}