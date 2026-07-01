package com.mopl.domain.content.service;


import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.dto.ContentUpdateRequest;

import com.mopl.global.response.CursorPageResponse;
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

  ContentResponse getContent(UUID id);

  //커서 페이지 네이션 + 검색/필터 조회 적용
  CursorPageResponse<ContentResponse> getContents(ContentSearchRequest request);

}
