package com.mopl.domain.review.controller;

import com.mopl.domain.review.dto.*;
import com.mopl.domain.review.service.ReviewService;
import com.mopl.global.response.CursorPageResponse;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping
  public ResponseEntity<UUID> createReview(@RequestBody @Valid ReviewCreateRequest request) {
    UUID reviewId = reviewService.createReview(
        request.contentId(), /* userId */ null, BigDecimal.valueOf(request.rating()), request.text());
    return ResponseEntity.status(HttpStatus.CREATED).body(reviewId);
  }

  @PatchMapping("/{reviewId}")
  public ResponseEntity<Void> updateReview(
      @PathVariable UUID reviewId, @RequestBody @Valid ReviewUpdateRequest request) {
    reviewService.updateReview(reviewId, /* userId */ null, BigDecimal.valueOf(request.rating()), request.text());
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{reviewId}")
  public ResponseEntity<Void> deleteReview(@PathVariable UUID reviewId) {
    reviewService.deleteReview(reviewId, /* userId */ null);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<CursorPageResponse<ReviewDto>> getReviews(
      @RequestParam UUID contentId,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam String sortDirection,
      @RequestParam String sortBy
  ) {
    ReviewSearchRequest request = new ReviewSearchRequest(
        contentId, cursor, idAfter, limit, sortBy, sortDirection);
    return ResponseEntity.ok(reviewService.getReviews(request));
  }
}