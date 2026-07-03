package com.mopl.domain.watchingsession.adapter.content;

import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.service.ContentUseCase;
import com.mopl.domain.watchingsession.adapter.port.LoadContentPort;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchingSessionContentAdapter implements LoadContentPort {

  private final ContentUseCase contentUseCase;

  @Override
  public ContentResponse getContent(UUID contentId) {
    return contentUseCase.getContent(contentId);
  }
}