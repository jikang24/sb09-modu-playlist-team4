package com.mopl.domain.content.service;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentUpdateRequest;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * 콘텐츠 서비스
 *
 * 순수 도메인 Content만 다룸
 * JPA, DB 기술을 직접 알지 못함 → ContentRepository(Port Out) 통해서만 접근
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService implements ContentUseCase {

  private final ContentRepository contentRepository;
  // TODO: S3Uploader 구현 완료 후 주입
  // private final S3Uploader s3Uploader;

  // ──────────────────────────────────────────────
  // 관리자 전용
  // ──────────────────────────────────────────────

  @Override
  @Transactional
  public ContentResponse createContent(ContentCreateRequest request, MultipartFile thumbnail) {

    contentRepository.findByTypeAndExternalId(request.type(), request.externalId())
        .ifPresent(c -> { throw new MoplException(ErrorCode.CONTENT_ALREADY_EXISTS); });

    // TODO: S3 업로드
    // String thumbnailUrl = s3Uploader.upload(thumbnail, "contents");
    String thumbnailUrl = null;

    // 순수 도메인 생성 (JPA 어노테이션 없음)
    Content content = Content.create(
        request.type(),
        request.externalId(),
        request.title(),
        request.description(),
        thumbnailUrl,
        request.tags()
    );

    Content saved = contentRepository.save(content);
    log.info("[Content] 등록 완료 - id: {}, type: {}", saved.getId(), saved.getType());
    return ContentResponse.from(saved);
  }

  @Override
  @Transactional
  public ContentResponse updateContent(UUID id, ContentUpdateRequest request,
      MultipartFile thumbnail) {
    Content content = findContentOrThrow(id);

    String thumbnailUrl = content.getThumbnailUrl();
    if (thumbnail != null && !thumbnail.isEmpty()) {
      // TODO: S3 업로드
      // thumbnailUrl = s3Uploader.upload(thumbnail, "contents");
    }

    // 순수 도메인 메서드로 상태 변경
    content.update(request.title(), request.description(), thumbnailUrl, request.tags());

    Content saved = contentRepository.save(content);
    log.info("[Content] 수정 완료 - id: {}", id);
    return ContentResponse.from(saved);
  }

  @Override
  @Transactional
  public void deleteContent(UUID id) {
    if (!contentRepository.existsById(id)) {
      throw new MoplException(ErrorCode.CONTENT_NOT_FOUND);
    }
    contentRepository.deleteById(id);
    log.info("[Content] 삭제 완료 - id: {}", id);
  }

  // ──────────────────────────────────────────────
  // 조회
  // ──────────────────────────────────────────────

  @Override
  public ContentResponse getContent(UUID id) {
    return ContentResponse.from(findContentOrThrow(id));
  }

  @Override
  public List<ContentResponse> getContents() {
    return contentRepository.findAll().stream()
        .map(ContentResponse::from)
        .toList();
  }

  @Override
  public List<ContentResponse> getContentsByType(ContentType type) {
    return contentRepository.findAllByType(type).stream()
        .map(ContentResponse::from)
        .toList();
  }

  // ──────────────────────────────────────────────
  // 이벤트 수신
  // ──────────────────────────────────────────────

  @EventListener
  @Transactional
  public void handleReviewRatingUpdated(ReviewRatingUpdatedEvent event) {
    Content content = findContentOrThrow(event.contentId());
    content.updateRatingStats(event.averageRating(), event.reviewCount());
    contentRepository.save(content);
    log.info("[Content] 평점 갱신 - id: {}", event.contentId());
  }

  // ──────────────────────────────────────────────
  // 내부 헬퍼
  // ──────────────────────────────────────────────

  private Content findContentOrThrow(UUID id) {
    return contentRepository.findById(id)
        .orElseThrow(() -> new MoplException(ErrorCode.CONTENT_NOT_FOUND));
  }
}