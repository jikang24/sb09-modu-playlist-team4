package com.mopl.domain.content.service;

import com.mopl.domain.content.adapter.port.LoadWatcherCountPort;
import com.mopl.domain.content.adapter.port.SearchContentPort;
import com.mopl.domain.content.adapter.port.SearchContentResult;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentSortField;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.dto.ContentUpdateRequest;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.config.PlaceholderImageController;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import com.mopl.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
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
  private final S3Service s3Service;
  private final SearchContentPort searchContentPort;

  // ──────────────────────────────────────────────
  // 관리자 전용
  // ──────────────────────────────────────────────

  @Override
  @Transactional
  public ContentResponse createContent(ContentCreateRequest request, MultipartFile thumbnail) {

    String thumbnailKey = s3Service.extractKey(s3Service.upload(thumbnail));

    // 관리자가 직접 등록하는 콘텐츠는 TMDB 등 외부 연동 ID가 없으므로 서버에서 고유값 생성
    String externalId = Content.MANUAL_EXTERNAL_ID_PREFIX + UUID.randomUUID();

    // 순수 도메인 생성 (JPA 어노테이션 없음)
    Content content = Content.create(
        request.type(),
        externalId,
        request.title(),
        request.description(),
        thumbnailKey,
        request.tags()
    );

    Content saved = contentRepository.save(content);

    searchContentPort.save(saved);

    log.info("[Content] 등록 완료 - id: {}, type: {}", saved.getId(), saved.getType());
    return ContentResponse.from(saved, loadWatcherCountPort.countByContentId(saved.getId()),
        resolveThumbnailUrl(saved.getThumbnailUrl()));
  }

  @Override
  @Transactional
  public ContentResponse updateContent(UUID id, ContentUpdateRequest request,
      MultipartFile thumbnail) {
    Content content = findContentOrThrow(id);

    String thumbnailKey = content.getThumbnailUrl();
    if (thumbnail != null && !thumbnail.isEmpty()) {
      thumbnailKey = s3Service.extractKey(s3Service.upload(thumbnail));
    }

    // 순수 도메인 메서드로 상태 변경
    content.update(request.type(), request.title(), request.description(), thumbnailKey, request.tags());

    Content saved = contentRepository.save(content);
    searchContentPort.save(saved);
    log.info("[Content] 수정 완료 - id: {}", id);
    return ContentResponse.from(saved, loadWatcherCountPort.countByContentId(saved.getId()),
        resolveThumbnailUrl(saved.getThumbnailUrl()));
  }

  @Override
  @Transactional
  public void deleteContent(UUID id) {
    if (!contentRepository.existsById(id)) {
      throw new MoplException(ErrorCode.CONTENT_NOT_FOUND);
    }
    contentRepository.deleteById(id);
    searchContentPort.delete(id);
    log.info("[Content] 삭제 완료 - id: {}", id);
  }

  @Override
  public ContentResponse getContent(UUID id) {
    Content content = findContentOrThrow(id);
    return ContentResponse.from(content, loadWatcherCountPort.countByContentId(id),
        resolveThumbnailUrl(content.getThumbnailUrl()));
  }

  @Override
  public CursorPageResponse<ContentResponse> getContents(ContentSearchRequest request) {
    // limit+1개 조회
    List<Content> contents;
    long totalCount;

    if (request.keywordLike() == null ||
        request.keywordLike().isBlank()) {

      contents = contentRepository.findAllByCondition(request);
      totalCount = contentRepository.countByCondition(request);

    } else {

      SearchContentResult result =
          searchContentPort.search(
              request.keywordLike(),
              request
          );

      contents =
          contentRepository.findAllByIdsWithCondition(
              result.ids(),
              request
          );

      totalCount = result.totalCount();
    }
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
        .map(c -> ContentResponse.from(c, watcherCounts.getOrDefault(c.getId(), 0L),
            resolveThumbnailUrl(c.getThumbnailUrl())))
        .toList();

    return new CursorPageResponse<>(
        data, nextCursor, nextIdAfter,
        hasNext, totalCount,
        request.sortBy(), request.sortDirection()
    );
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional
  public void handleReviewRatingUpdated(ReviewRatingUpdatedEvent event) {
    Content content = findContentOrThrow(event.contentId());
    content.updateRatingStats(event.averageRating(), event.reviewCount());
    Content saved = contentRepository.save(content);

    try {
      searchContentPort.save(saved);
    } catch (Exception e) {
      log.error("[Content] OpenSearch 평점 갱신 실패 - id: {}", event.contentId(), e);
    }
    log.info("[Content] 평점 갱신 - id: {}", event.contentId());
  }

  private Content findContentOrThrow(UUID id) {
    return contentRepository.findById(id)
        .orElseThrow(() -> new MoplException(ErrorCode.CONTENT_NOT_FOUND));
  }

  // 저장된 값에 스킴("://")이 없으면 우리가 업로드한 S3 key이므로 presigned URL로 치환하고,
  // 스킴이 있으면(TMDB 등 외부 썸네일 URL) 그대로 반환한다.
  // 저장된 썸네일이 아예 없는 경우, null을 그대로 내려보내면 프론트 컴포넌트에 따라
  // <img> 자체를 렌더링하지 않아(관리자 Preview 등) 깨진 화면으로 보일 수 있으므로
  // 항상 유효한 이미지 URL(fallback)을 응답한다.
  private String resolveThumbnailUrl(String stored) {
    if (stored == null || stored.isBlank()) {
      return PlaceholderImageController.PATH;
    }
    if (stored.contains("://")) {
      return stored;
    }
    return s3Service.getPresignedUrl(stored);
  }
}