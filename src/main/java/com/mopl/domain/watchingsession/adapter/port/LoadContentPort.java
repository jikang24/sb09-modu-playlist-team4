package com.mopl.domain.watchingsession.adapter.port;

import com.mopl.domain.content.dto.ContentResponse;
import java.util.UUID;

public interface LoadContentPort {
  ContentResponse getContent(UUID contentId);
}