package com.mopl.domain.playlist.adapter.out.content;

import com.mopl.global.dto.ContentSummary;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.playlist.application.port.out.LoadContentPort;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ContentAdapter implements LoadContentPort {

  private final ContentRepository contentRepository;

  @Override
  public boolean existsById(UUID contentId) {
    return contentRepository.existsById(contentId);
  }

  @Override
  public List<ContentSummary> findSummariesByIds(List<UUID> contentIds) {
    if (contentIds == null || contentIds.isEmpty()) {
      return List.of();
    }
    List<ContentSummary> summaries = new ArrayList<>();
    for (UUID contentId : contentIds) {
      contentRepository.findById(contentId)
          .ifPresent(content -> summaries.add(
              new ContentSummary(
                  content.getId(),
                  content.getType().name(),
                  content.getTitle(),
                  content.getDescription(),
                  content.getThumbnailUrl(),
                  content.getTags(),
                  content.getAverageRating(),
                  content.getReviewCount()
              )
          ));
    }
    return summaries;
  }
}
