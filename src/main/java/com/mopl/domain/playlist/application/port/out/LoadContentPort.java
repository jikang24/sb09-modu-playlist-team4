package com.mopl.domain.playlist.application.port.out;

import com.mopl.global.dto.ContentSummary;
import java.util.List;
import java.util.UUID;

public interface LoadContentPort {

  boolean existsById(UUID contentId);

  List<ContentSummary> findSummariesByIds(List<UUID> contentIds);
}
