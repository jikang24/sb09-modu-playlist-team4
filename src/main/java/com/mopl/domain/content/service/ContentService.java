package com.mopl.domain.content.service;

import com.mopl.domain.content.adapter.port.LoadWatcherCountPort;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentSortField;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.dto.ContentUpdateRequest;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
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
  private final LoadWatcherCountPort loadWatcherCountPort;
  // TODO: S3Uploader 구현 완료 후 주입
  // private final S3Uploader s3Uploader;

  // ──────────────────────────────────────────────
  // 관리자 전용
  // ──────────────────────────────────────────────

  @Override
  @Transactional
  public ContentResponse createContent(ContentCreateRequest request, MultipartFile thumbnail) {

    // TODO: S3 업로드
    // String thumbnailUrl = s3Uploader.upload(thumbnail, "contents");
    String thumbnailUrl = null;

    // 관리자가 직접 등록하는 콘텐츠는 TMDB 등 외부 연동 ID가 없으므로 서버에서 고유값 생성
    String externalId = Content.MANUAL_EXTERNAL_ID_PREFIX + UUID.randomUUID();

    // 순수 도메인 생성 (JPA 어노테이션 없음)
    Content content = Content.create(
        request.type(),
        externalId,
        request.title(),
        request.description(),
        thumbnailUrl,
        request.tags()
    );

    Content saved = contentRepository.save(content);
    log.info("[Content] 등록 완료 - id: {}, type: {}", saved.getId(), saved.getType());
    return ContentResponse.from(saved, loadWatcherCountPort.countByContentId(saved.getId()));
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
    content.update(request.type(), request.title(), request.description(), thumbnailUrl, request.tags());

    Content saved = contentRepository.save(content);
    log.info("[Content] 수정 완료 - id: {}", id);
    return ContentResponse.from(saved, loadWatcherCountPort.countByContentId(saved.getId()));
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

  @Override
  public ContentResponse getContent(UUID id) {
    Content content = findContentOrThrow(id);
    return ContentResponse.from(content, loadWatcherCountPort.countByContentId(id));
  }

  @Override
  public CursorPageResponse<ContentResponse> getContents(ContentSearchRequest request) {
    // limit+1개 조회
    List<Content> contents = contentRepository.findAllByCondition(request);
    long totalCount = contentRepository.countByCondition(request);

    // hasNext 판단: limit+1개가 조회됐으면 다음 페이지 있음
    boolean hasNext = contents.size() > request.limit();

    // 실제 반환할 데이터는 limit개만
    List<Content> pageData = hasNext
        ? contents.subList(0, request.limit())
        : contents;

    // 다음 커서: 마지막 항목의 정렬 기준 필드 값 + id (정렬 기준과 커서 기준이 항상 같아야 함)
    ContentSortField sortField = ContentSortField.resolve(request.sortBy());
    String nextCursor = null;
    UUID nextIdAfter = null;
    if (hasNext && !pageData.isEmpty()) {
      Content last = pageData.get(pageData.size() - 1);
      nextCursor = switch (sortField) {
        case REVIEW_COUNT -> String.valueOf(last.getReviewCount());
        case AVERAGE_RATING -> last.getAverageRating().toString();
        case CREATED_AT -> last.getCreatedAt().toString(); // ISO 8601 문자열
      };
      nextIdAfter = last.getId();
    }
    // 콘텐츠마다 개별 조회하지 않도록 페이지 안 콘텐츠 전체의 시청자 수를 한 번에 조회 (N+1 방지)
    List<UUID> contentIds = pageData.stream().map(Content::getId).toList();
    Map<UUID, Long> watcherCounts = loadWatcherCountPort.countByContentIds(contentIds);

    List<ContentResponse> data = pageData.stream()
        .map(c -> ContentResponse.from(c, watcherCounts.getOrDefault(c.getId(), 0L)))
        .toList();

    return new CursorPageResponse<>(
        data, nextCursor, nextIdAfter,
        hasNext, totalCount,
        request.sortBy(), request.sortDirection()
    );
  }

  @EventListener
  @Transactional
  public void handleReviewRatingUpdated(ReviewRatingUpdatedEvent event) {
    Content content = findContentOrThrow(event.contentId());
    content.updateRatingStats(event.averageRating(), event.reviewCount());
    contentRepository.save(content);
    log.info("[Content] 평점 갱신 - id: {}", event.contentId());
  }

  private Content findContentOrThrow(UUID id) {
    return contentRepository.findById(id)
        .orElseThrow(() -> new MoplException(ErrorCode.CONTENT_NOT_FOUND));
  }
}