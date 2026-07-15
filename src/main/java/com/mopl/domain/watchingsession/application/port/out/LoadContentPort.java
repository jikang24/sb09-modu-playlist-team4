package com.mopl.domain.watchingsession.application.port.out;

import com.mopl.global.dto.ContentSummary;
import java.util.UUID;

public interface LoadContentPort {
  ContentSummary getContent(UUID contentId);
}