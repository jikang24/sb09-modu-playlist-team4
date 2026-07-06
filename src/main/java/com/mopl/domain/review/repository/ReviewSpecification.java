package com.mopl.domain.review.repository;

import com.mopl.domain.review.domain.Review;
import com.mopl.domain.review.dto.ReviewSearchRequest;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * 커서 기반 페이지네이션용 동적 쿼리
 * sortBy가 "createdAt"이냐 "rating"이냐에 따라 cursor 파싱 타입이 다르므로 if로 분기함
 * (정렬 기준이 딱 2개뿐이고 당장 늘어날 계획 없어서, 굳이 제네릭으로 추상화 안 하기로 결정함)
 */
public class ReviewSpecification {

  public static Specification<Review> byCondition(ReviewSearchRequest request) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // 필수 조건: 특정 콘텐츠의 리뷰만 조회
      predicates.add(cb.equal(root.get("contentId"), request.contentId()));

      if (request.cursor() != null && request.idAfter() != null) {
        boolean isAscending = "ASCENDING".equalsIgnoreCase(request.sortDirection());
        String sortBy = request.sortBy() != null ? request.sortBy() : "createdAt";

        predicates.add("rating".equals(sortBy)
            ? buildRatingCursorPredicate(root, cb, request, isAscending)
            : buildCreatedAtCursorPredicate(root, cb, request, isAscending));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /** createdAt 기준 커서 비교 - "이 시각보다 이후(또는 이전) 것, 같으면 id로 순서 고정" */
  private static Predicate buildCreatedAtCursorPredicate(
      Root<Review> root, CriteriaBuilder cb, ReviewSearchRequest request, boolean isAscending) {
    Instant cursorTime;
    try {
      cursorTime = Instant.parse(request.cursor());
    } catch (DateTimeParseException e) {
      throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
    }

    return isAscending
        ? cb.or(cb.greaterThan(root.get("createdAt"), cursorTime),
        cb.and(cb.equal(root.get("createdAt"), cursorTime),
            cb.greaterThan(root.get("id"), request.idAfter())))
        : cb.or(cb.lessThan(root.get("createdAt"), cursorTime),
            cb.and(cb.equal(root.get("createdAt"), cursorTime),
                cb.lessThan(root.get("id"), request.idAfter())));
  }

  /** rating 기준 커서 비교 - 구조는 createdAt과 동일, 타입만 BigDecimal로 다름 */
  private static Predicate buildRatingCursorPredicate(
      Root<Review> root, CriteriaBuilder cb, ReviewSearchRequest request, boolean isAscending) {
    BigDecimal cursorRating;
    try {
      cursorRating = new BigDecimal(request.cursor());
    } catch (NumberFormatException e) {
      throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
    }

    return isAscending
        ? cb.or(cb.greaterThan(root.get("rating"), cursorRating),
        cb.and(cb.equal(root.get("rating"), cursorRating),
            cb.greaterThan(root.get("id"), request.idAfter())))
        : cb.or(cb.lessThan(root.get("rating"), cursorRating),
            cb.and(cb.equal(root.get("rating"), cursorRating),
                cb.lessThan(root.get("id"), request.idAfter())));
  }
}