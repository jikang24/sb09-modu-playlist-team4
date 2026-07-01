package com.mopl.domain.content.repository;

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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ContentSpecification {

  public static Specification<ContentJpaEntity> byCondition(ContentSearchRequest request) {
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

      if (request.cursor() != null && request.idAfter() != null) {
        // 커서 포맷 검증 - 잘못된 형식이면 400 반환
        Instant cursorTime;
        try {
          cursorTime = Instant.parse(request.cursor());
        } catch (DateTimeParseException e) {
          throw new MoplException(ErrorCode.INVALID_CURSOR_FORMAT);
        }

        boolean isAscending = "ASCENDING".equalsIgnoreCase(request.sortDirection());

        if (isAscending) {
          predicates.add(cb.or(
              cb.greaterThan(root.get("createdAt"), cursorTime),
              cb.and(
                  cb.equal(root.get("createdAt"), cursorTime),
                  cb.greaterThan(root.get("id"), request.idAfter())
              )
          ));
        } else {
          predicates.add(cb.or(
              cb.lessThan(root.get("createdAt"), cursorTime),
              cb.and(
                  cb.equal(root.get("createdAt"), cursorTime),
                  cb.lessThan(root.get("id"), request.idAfter())
              )
          ));
        }
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}