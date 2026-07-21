package com.mopl.domain.content.controller;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.dto.ContentUpdateRequest;
import com.mopl.domain.content.service.ContentUseCase;
import com.mopl.global.response.CursorPageResponse;
import com.mopl.infra.s3.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "콘텐츠")
@RestController
@RequestMapping("/api/contents")
@RequiredArgsConstructor
public class ContentController {

  private final ContentUseCase contentUseCase;
  private final S3Service s3Service;

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
  @Operation(summary = "콘텐츠 등록", description = "관리자만 등록할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "403", description = "권한 오류"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ContentResponse> createContent(
      @RequestPart("request") @Valid ContentCreateRequest request,
      @RequestPart("thumbnail") MultipartFile thumbnail) {

    // S3 업로드는 DB 트랜잭션 밖(컨트롤러)에서 수행 - 외부 스토리지 지연/장애가 콘텐츠 등록 트랜잭션을 붙잡지 않도록
    String thumbnailKey = s3Service.extractKey(s3Service.upload(thumbnail));
    ContentResponse response = contentUseCase.createContent(request, thumbnailKey);
    return ResponseEntity.status(201).body(response);
  }

  /**
   * 콘텐츠 수정
   *  * 콘텐츠 수정 (관리자 전용)
   *      * PATCH /api/contents/{id}
   *      * Content-Type: multipart/form-data
   *      *
   *      * thumbnail은 optional (변경 안 할 수도 있음)
   */
  @Operation(summary = "콘텐츠 수정", description = "관리자만 수정할 수 있습니다. thumbnail은 선택이며, 보내지 않으면 기존 값이 유지됩니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "403", description = "권한 오류"),
      @ApiResponse(responseCode = "404", description = "해당 콘텐츠 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ContentResponse> updateContent(
      @PathVariable UUID id,
      @RequestPart("request") @Valid ContentUpdateRequest request,
      @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail) {

    // 새 썸네일이 없으면 null 그대로 넘겨 기존 값 유지 - S3 업로드는 트랜잭션 밖(컨트롤러)에서 수행
    String newThumbnailKey = (thumbnail != null && !thumbnail.isEmpty())
        ? s3Service.extractKey(s3Service.upload(thumbnail))
        : null;
    ContentResponse response = contentUseCase.updateContent(id, request, newThumbnailKey);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "콘텐츠 삭제", description = "관리자만 삭제할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "성공"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "403", description = "권한 오류"),
      @ApiResponse(responseCode = "404", description = "해당 콘텐츠 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteContent(@PathVariable UUID id) {
    contentUseCase.deleteContent(id);
    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "콘텐츠 상세 조회")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "404", description = "해당 콘텐츠 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @GetMapping("/{id}")
  public ResponseEntity<ContentResponse> getContent(@PathVariable UUID id) {
    return ResponseEntity.ok(contentUseCase.getContent(id));
  }

  @Operation(summary = "콘텐츠 목록 조회", description = "타입/키워드/태그 필터와 커서 기반 페이지네이션을 지원합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @GetMapping
  public ResponseEntity<CursorPageResponse<ContentResponse>> getContents(
      @RequestParam(required = false) ContentType typeEqual,
      @RequestParam(required = false) String keywordLike,
      @RequestParam(required = false) List<String> tagsIn,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) UUID idAfter,
      @RequestParam int limit,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "DESCENDING") String sortDirection) {

    ContentSearchRequest request = new ContentSearchRequest(
        typeEqual, keywordLike, tagsIn,
        cursor, idAfter, limit, sortBy, sortDirection
    );

    return ResponseEntity.ok(contentUseCase.getContents(request));
  }
}