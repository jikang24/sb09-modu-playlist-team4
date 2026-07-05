package com.mopl.domain.playlist.application.port.out;

import com.mopl.global.dto.UserSummary;
import java.util.UUID;

public interface LoadUserPort {

  UserSummary getUserSummary(UUID userId);
}
