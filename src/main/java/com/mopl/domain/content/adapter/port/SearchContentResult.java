package com.mopl.domain.content.adapter.port;

import java.util.List;
import java.util.UUID;

public record SearchContentResult(
    List<UUID> ids,
    long totalCount
) {}