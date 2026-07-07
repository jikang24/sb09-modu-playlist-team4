package com.mopl.domain.review.repository;

import com.mopl.domain.review.domain.QReview;
import com.mopl.domain.review.domain.Review;
import com.mopl.domain.review.dto.ReviewSearchRequest;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 커서 기반 페이지네이션용 동적 쿼리
 * sortBy가 "createdAt"이냐 "rating"이냐에 따라 cursor 파싱 타입이 다르므로 if로 분기함
 * (정렬 기준이 딱 2개뿐이고 당장 늘어날 계획 없어서, 굳이 제네릭으로 추상화 안 하기로 결정함)
 */
@Repository
@RequiredArgsConstructor
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Review> findAllByCondition(ReviewSearchRequest request) {
    QReview review = QReview.review;
    boolean isAscending = "ASCENDING".equalsIgnoreCase(request.sortDirection());
    String sortBy = request.sortBy() != null ? request.sortBy() : "createdAt";

    BooleanBuilder builder = new BooleanBuilder()
        .and(review.contentId.eq(request.contentId()));

    if (request.cursor() != null && request.idAfter() != null) {
      builder.and("rating".equals(sortBy)
          ? ratingCursorPredicate(review, request, isAscending)
          : createdAtCursorPredicate(review, request, isAscending));
    }

    OrderSpecifier<?> primaryOrder = "rating".equals(sortBy)
        ? (isAscending ? review.rating.asc() : review.rating.desc())
        : (isAscending ? review.createdAt.asc() : review.createdAt.desc());
    OrderSpecifier<?> idOrder = isAscending ? review.id.asc() : review.id.desc();

    return queryFactory.selectFrom(review)
        .where(builder)
        .orderBy(primaryOrder, idOrder)
        .limit(request.limit() + 1L) // limit+1개 조회 → hasNext 판단용
        .fetch();
  }

  @Override
  public long countByCondition(ReviewSearchRequest request) {
    QReview review = QReview.review;

    Long count = queryFactory.select(review.count())
        .from(review)
        .where(review.contentId.eq(request.contentId()))
        .fetchOne();

    return count != null ? count : 0L;
  }

  /** createdAt 기준 커서 비교 - "이 시각보다 이후(또는 이전) 것, 같으면 id로 순서 고정" */
  private BooleanExpression createdAtCursorPredicate(
      QReview review, ReviewSearchRequest request, boolean isAscending) {
    Instant cursorTime;
    try {
      cursorTime = Instant.parse(request.cursor());
    } catch (DateTimeParseException e) {
      throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
    }

    return isAscending
        ? review.createdAt.gt(cursorTime)
            .or(review.createdAt.eq(cursorTime).and(review.id.gt(request.idAfter())))
        : review.createdAt.lt(cursorTime)
            .or(review.createdAt.eq(cursorTime).and(review.id.lt(request.idAfter())));
  }

  /** rating 기준 커서 비교 - 구조는 createdAt과 동일, 타입만 BigDecimal로 다름 */
  private BooleanExpression ratingCursorPredicate(
      QReview review, ReviewSearchRequest request, boolean isAscending) {
    BigDecimal cursorRating;
    try {
      cursorRating = new BigDecimal(request.cursor());
    } catch (NumberFormatException e) {
      throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
    }

    return isAscending
        ? review.rating.gt(cursorRating)
            .or(review.rating.eq(cursorRating).and(review.id.gt(request.idAfter())))
        : review.rating.lt(cursorRating)
            .or(review.rating.eq(cursorRating).and(review.id.lt(request.idAfter())));
  }
}