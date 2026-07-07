package com.mopl.domain.content.repository;

import com.mopl.domain.content.domain.ContentSortField;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.infrastructure.ContentJpaEntity;
import com.mopl.domain.content.infrastructure.ContentTagJpaEntity;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.format.DateTimeParseException;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ContentSpecification {

  /** 커서를 뺀 필터 조건만 (totalCount 계산용 - 페이지와 무관하게 항상 같아야 함) */
  public static Specification<ContentJpaEntity> byFilter(ContentSearchRequest request) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (request.typeEqual() != null) {
        predicates.add(cb.equal(root.get("type"), request.typeEqual()));
      }

      if (request.keywordLike() != null && !request.keywordLike().isBlank()) {
        predicates.add(cb.like(
            cb.lower(root.get("title")),
            "%" + request.keywordLike().toLowerCase() + "%"
        ));
      }

      if (request.tagsIn() != null && !request.tagsIn().isEmpty()) {
        Subquery<ContentTagJpaEntity> subquery =
            query.subquery(ContentTagJpaEntity.class);
        Root<ContentTagJpaEntity> tagRoot =
            subquery.from(ContentTagJpaEntity.class);

        subquery.select(tagRoot)
            .where(
                cb.and(
                    // 현재 조회 중인 content와 연결
                    cb.equal(tagRoot.get("content"), root),
                    // 요청한 태그 목록 중 하나라도 포함
                    tagRoot.get("tag").in(request.tagsIn())
                )
            );

        predicates.add(cb.exists(subquery));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /** 필터 + 커서 조건 (목록 조회용) - sortField는 실제 정렬에 쓰인 필드와 반드시 일치해야 함 */
  public static Specification<ContentJpaEntity> byCondition(
      ContentSearchRequest request, ContentSortField sortField) {
    Specification<ContentJpaEntity> spec = byFilter(request);

    if (request.cursor() != null && request.idAfter() != null) {
      spec = spec.and(cursorPredicate(request, sortField));
    }

    return spec;
  }

  private static Specification<ContentJpaEntity> cursorPredicate(
      ContentSearchRequest request, ContentSortField sortField) {
    return (root, query, cb) -> {
      boolean isAscending = "ASCENDING".equalsIgnoreCase(request.sortDirection());
      String property = sortField.propertyName();

      if (sortField == ContentSortField.REVIEW_COUNT) {
        int cursorValue;
        try {
          cursorValue = Integer.parseInt(request.cursor());
        } catch (NumberFormatException e) {
          throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
        }

        return isAscending
            ? cb.or(
                cb.greaterThan(root.get(property), cursorValue),
                cb.and(
                    cb.equal(root.get(property), cursorValue),
                    cb.greaterThan(root.get("id"), request.idAfter())))
            : cb.or(
                cb.lessThan(root.get(property), cursorValue),
                cb.and(
                    cb.equal(root.get(property), cursorValue),
                    cb.lessThan(root.get("id"), request.idAfter())));
      }

      if (sortField == ContentSortField.AVERAGE_RATING) {
        BigDecimal cursorValue;
        try {
          cursorValue = new BigDecimal(request.cursor());
        } catch (NumberFormatException e) {
          throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
        }

        return isAscending
            ? cb.or(
                cb.greaterThan(root.get(property), cursorValue),
                cb.and(
                    cb.equal(root.get(property), cursorValue),
                    cb.greaterThan(root.get("id"), request.idAfter())))
            : cb.or(
                cb.lessThan(root.get(property), cursorValue),
                cb.and(
                    cb.equal(root.get(property), cursorValue),
                    cb.lessThan(root.get("id"), request.idAfter())));
      }

      // 기본: createdAt 기준 커서
      Instant cursorTime;
      try {
        cursorTime = Instant.parse(request.cursor());
      } catch (DateTimeParseException e) {
        throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
      }

      return isAscending
          ? cb.or(
              cb.greaterThan(root.get(property), cursorTime),
              cb.and(
                  cb.equal(root.get(property), cursorTime),
                  cb.greaterThan(root.get("id"), request.idAfter())))
          : cb.or(
              cb.lessThan(root.get(property), cursorTime),
              cb.and(
                  cb.equal(root.get(property), cursorTime),
                  cb.lessThan(root.get("id"), request.idAfter())));
    };
  }
}