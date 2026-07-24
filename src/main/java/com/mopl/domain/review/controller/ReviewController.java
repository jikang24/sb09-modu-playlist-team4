package com.mopl.domain.review.controller;

import com.mopl.domain.review.dto.*;
import com.mopl.domain.review.service.ReviewService;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "리뷰 관리")
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @Operation(summary = "리뷰 작성", description = "콘텐츠 1개당 본인 리뷰는 1개만 작성할 수 있습니다.")
  @PostMapping
  public ResponseEntity<UUID> createReview(
      @RequestBody @Valid ReviewCreateRequest request, @AuthenticationPrincipal JwtClaims claims) {
    UUID reviewId = reviewService.createReview(
        request.contentId(), claims.getUserId(), BigDecimal.valueOf(request.rating()), request.text());
    return ResponseEntity.status(HttpStatus.CREATED).body(reviewId);
  }

  @Operation(summary = "리뷰 수정", description = "작성자 본인만 수정할 수 있습니다.")
  @PatchMapping("/{reviewId}")
  public ResponseEntity<Void> updateReview(
      @PathVariable UUID reviewId, @RequestBody @Valid ReviewUpdateRequest request,
      @AuthenticationPrincipal JwtClaims claims) {
    reviewService.updateReview(
        reviewId, claims.getUserId(), BigDecimal.valueOf(request.rating()), request.text());
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "리뷰 삭제", description = "작성자 본인만 삭제할 수 있습니다.")
  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(
      @PathVariable UUID reviewId, @AuthenticationPrincipal JwtClaims claims) {
    reviewService.deleteReview(reviewId, claims.getUserId());
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "리뷰 목록 조회 (커서 페이지네이션)")
  @GetMapping
  public ResponseEntity<CursorPageResponse<ReviewDto>> getReviews(
      @RequestParam UUID contentId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam SortDirection sortDirection,
      @RequestParam ReviewSortBy sortBy
  ) {
    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, cursor, idAfter, limit, sortBy, sortDirection);
    return ResponseEntity.ok(reviewService.getReviews(request));
  }
}