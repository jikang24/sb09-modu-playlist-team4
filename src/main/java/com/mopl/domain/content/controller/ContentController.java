package com.mopl.domain.content.controller;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentUpdateRequest;
import com.mopl.domain.content.service.ContentUseCase;
import com.mopl.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

/**
 * [Adapter In] 콘텐츠 컨트롤러
 *
 * HTTP 요청을 받아 ContentUseCase(Port In)에 전달
 * - ContentService 직접 의존 X → ContentUseCase 인터페이스에만 의존
 * - 비즈니스 로직 없이 요청/응답 변환만 담당
 *
 *  * 프론트 코드 분석 결과에 맞춰 수정:
 *  * 1. /api/admin/contents → /api/contents (프론트가 admin prefix 없이 호출)
 *  * 2. 생성/수정 → multipart/form-data 방식
 *  *    - "request" 파트: JSON (ContentCreateRequest)
 *  *    - "thumbnail" 파트: 이미지 파일 (S3 업로드)
 */
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

  private final ContentUseCase contentUseCase;

  // ──────────────────────────────────────────────
  // 관리자 전용 API
  // ──────────────────────────────────────────────

  /**
   * 콘텐츠 등록 (관리자 전용)
   * Content-Type: multipart/form-data
   *
   * 프론트 호출 방식:
   *   formData.append("request", new Blob([JSON.stringify(request)], { type: "application/json" }))
   *   formData.append("thumbnail", file)
   */
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ContentResponse>> createContent(
      @RequestPart("request") @Valid ContentCreateRequest request,
      @RequestPart("thumbnail") MultipartFile thumbnail) {

    ContentResponse response = contentUseCase.createContent(request, thumbnail);
    return ResponseEntity.status(201).body(ApiResponse.created(response));
  }

  /**
   * 콘텐츠 수정
   *  * 콘텐츠 수정 (관리자 전용)
   *      * PATCH /api/contents/{id}
   *      * Content-Type: multipart/form-data
   *      *
   *      * thumbnail은 optional (변경 안 할 수도 있음)
   */
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<ContentResponse>> updateContent(
      @PathVariable UUID id,
      @RequestPart("request") @Valid ContentUpdateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {

    ContentResponse response = contentUseCase.updateContent(id, request, thumbnail);
    return ResponseEntity.ok(ApiResponse.ok(response));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deleteContent(@PathVariable UUID id) {
    contentUseCase.deleteContent(id);
    return ResponseEntity.ok(ApiResponse.noContent());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ContentResponse>> getContent(@PathVariable UUID id) {
    return ResponseEntity.ok(ApiResponse.ok(contentUseCase.getContent(id)));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<ContentResponse>>> getContents(
      @RequestParam(required = false) ContentType type) {

    List<ContentResponse> responses = (type != null)
        ? contentUseCase.getContentsByType(type)
        : contentUseCase.getContents();

    return ResponseEntity.ok(ApiResponse.ok(responses));
  }
}
