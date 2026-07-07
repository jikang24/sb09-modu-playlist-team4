package com.mopl.domain.contentchat.adapter.port;

import com.mopl.global.dto.UserSummary;
import java.util.UUID;

public interface LoadUserPort {
  UserSummary getUserSummary(UUID userId);
}