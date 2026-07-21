package com.mopl.domain.playlist.adapter.out.content;

import com.mopl.global.config.PlaceholderImageController;
import com.mopl.global.dto.ContentSummary;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.playlist.application.port.out.LoadContentPort;
import com.mopl.infra.s3.S3Service;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentAdapter implements LoadContentPort {

  private final ContentRepository contentRepository;
  private final S3Service s3Service;

  @Override
  public boolean existsById(UUID contentId) {
    return contentRepository.existsById(contentId);
  }

  @Override
  public List<ContentSummary> findSummariesByIds(List<UUID> contentIds) {
    if (contentIds == null || contentIds.isEmpty()) {
      return List.of();
    }
    return contentRepository.findAllByIds(contentIds).stream()
        .map(content -> new ContentSummary(
            content.getId(),
            content.getType(),
            content.getTitle(),
            content.getDescription(),
            resolveThumbnailUrl(content.getThumbnailUrl()),
            content.getTags(),
            content.getAverageRating(),
            content.getReviewCount()
        ))
        .toList();
  }

  // 저장된 값에 스킴("://")이 없으면 우리가 업로드한 S3 key이므로 presigned URL로 치환하고,
  // 스킴이 있으면(TMDB 등 외부 썸네일 URL) 그대로 반환한다. (ContentService.resolveThumbnailUrl과 동일 규칙)
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
