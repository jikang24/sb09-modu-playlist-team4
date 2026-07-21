package com.mopl.domain.review.repository;

import com.mopl.domain.review.domain.Review;
import com.mopl.domain.review.dto.ReviewSearchRequest;
import java.util.List;

public interface ReviewRepositoryCustom {

  /** limit+1개 조회 (hasNext 판단용), 커서/정렬 조건 적용 */
  List<Review> findAllByCondition(ReviewSearchRequest request);

  /** 커서는 빼고 콘텐츠 조건만으로 카운트 (페이지가 넘어가도 totalCount는 동일해야 함) */
  long countByCondition(ReviewSearchRequest request);
}