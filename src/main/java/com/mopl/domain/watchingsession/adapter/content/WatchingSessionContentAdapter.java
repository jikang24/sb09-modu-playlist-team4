package com.mopl.domain.watchingsession.adapter.content;

import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.service.ContentUseCase;
import com.mopl.domain.watchingsession.application.port.out.LoadContentPort;
import com.mopl.global.dto.ContentSummary;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchingSessionContentAdapter implements LoadContentPort {

  private final ContentUseCase contentUseCase;

  @Override
  public ContentSummary getContent(UUID contentId) {
    ContentResponse content = contentUseCase.getContent(contentId);
    return new ContentSummary(
        content.id(),
        content.type(),
        content.title(),
        content.description(),
        content.thumbnailUrl(),
        content.tags(),
        content.averageRating(),
        content.reviewCount()
    );
  }
}