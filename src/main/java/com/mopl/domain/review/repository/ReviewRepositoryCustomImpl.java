package com.mopl.domain.review.repository;

import com.mopl.domain.review.domain.QReview;
import com.mopl.domain.review.domain.Review;
import com.mopl.domain.review.dto.ReviewSearchRequest;
import com.mopl.domain.review.dto.ReviewSortBy;
import com.mopl.global.dto.SortDirection;
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
 * sortBy(ReviewSortBy)가 CREATED_AT이냐 RATING이냐에 따라 커서 파싱 타입과 정렬 필드가 다르므로
 * switch로 분기함 (정렬 기준이 딱 2개뿐이고 당장 늘어날 계획 없어서, 굳이 제네릭으로 추상화 안 하기로 결정함)
 */
@Repository
@RequiredArgsConstructor
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<Review> findAllByCondition(ReviewSearchRequest request) {
    QReview review = QReview.review;
    boolean isAscending = request.sortDirection() == SortDirection.ASCENDING;
    ReviewSortBy sortBy = request.sortBy();

    BooleanBuilder builder = new BooleanBuilder()
        .and(review.contentId.eq(request.contentId()));

    if (request.cursor() != null && request.idAfter() != null) {
      // switch로 명확하게 분기 - 새로운 정렬 기준 추가 시 컴파일러가 누락된 case를 잡아줌
      builder.and(switch (sortBy) {
        case RATING -> ratingCursorPredicate(review, request, isAscending);
        case CREATED_AT -> createdAtCursorPredicate(review, request, isAscending);
      });
    }

    // 중첩 삼항연산자 대신 switch로 분리 - sortBy(2단계)와 isAscending(1단계)을 한 번에 섞지 않음
    OrderSpecifier<?> primaryOrder = switch (sortBy) {
      case RATING -> isAscending ? review.rating.asc() : review.rating.desc();
      case CREATED_AT -> isAscending ? review.createdAt.asc() : review.createdAt.desc();
    };
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