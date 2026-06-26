package com.mopl.domain.content.service;


import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentUpdateRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ContentUseCase {

  /** 콘텐츠 등록 (관리자 전용) */
  ContentResponse createContent(ContentCreateRequest request, MultipartFile thumbnailUrl);

  /** 콘텐츠 수정 (관리자 전용) */
  ContentResponse updateContent(UUID id, ContentUpdateRequest request, MultipartFile thumbnail);

  /** 콘텐츠 삭제 (관리자 전용) */
  void deleteContent(UUID id);

  /** 콘텐츠 단건 조회 */
  ContentResponse getContent(UUID id);

  /** 전체 콘텐츠 목록 조회 */
  List<ContentResponse> getContents();

  /** 타입별 콘텐츠 목록 조회 */
  List<ContentResponse> getContentsByType(ContentType type);

  /**
   * 평점/리뷰 수 갱신
   * Review 모듈의 이벤트를 받아 ContentEventListener가 호출
   */
  void updateRatingStats(UUID contentId, BigDecimal newAverageRating, int newReviewCount);

}
