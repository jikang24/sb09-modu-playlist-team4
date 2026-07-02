package com.mopl.domain.notification.support;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

  public UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null ||
        !(authentication.getPrincipal() instanceof JwtClaims claims)) {
      throw new MoplException(ErrorCode.INVALID_TOKEN);
    }

    return claims.getUserId();
  }
}