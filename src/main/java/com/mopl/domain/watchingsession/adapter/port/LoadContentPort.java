package com.mopl.domain.watchingsession.adapter.port;

import com.mopl.global.dto.ContentSummary;
import java.util.UUID;

public interface LoadContentPort {
  ContentSummary getContent(UUID contentId);
}