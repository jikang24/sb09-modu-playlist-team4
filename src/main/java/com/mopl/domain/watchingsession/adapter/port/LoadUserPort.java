package com.mopl.domain.watchingsession.adapter.port;

import com.mopl.global.dto.UserSummary;
import java.util.UUID;

public interface LoadUserPort {
  UserSummary getUserSummary(UUID userId);
}