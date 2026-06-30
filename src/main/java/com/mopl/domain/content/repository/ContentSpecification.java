package com.mopl.domain.content.repository;

import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.infrastructure.ContentJpaEntity;
import com.mopl.domain.content.infrastructure.ContentTagJpaEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 콘텐츠 동적 쿼리 Specification
 */
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
        Join<ContentJpaEntity, ContentTagJpaEntity> tagJoin =
            root.join("tags", JoinType.INNER);
        predicates.add(tagJoin.get("tag").in(request.tagsIn()));
        query.distinct(true); // 태그 join으로 중복 방지
      }

      if (request.cursor() != null && request.idAfter() != null) {
        Instant cursorTime = Instant.parse(request.cursor());

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