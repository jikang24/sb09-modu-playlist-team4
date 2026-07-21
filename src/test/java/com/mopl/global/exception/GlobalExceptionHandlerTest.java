package com.mopl.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("존재하지 않는 정적 리소스 - 404를 반환한다 (최후 안전망에 500으로 삼켜지면 안 됨)")
  void handleNoResourceFound_returns404() {
    NoResourceFoundException e =
        new NoResourceFoundException(HttpMethod.GET, "/placeholder-movie.png");

    ResponseEntity<ErrorResponse> response = handler.handleNoResourceFound(e);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
  }

  @Test
  @DisplayName("@PreAuthorize 권한 거부 - 403을 반환한다 (최후 안전망에 500으로 삼켜지면 안 됨)")
  void handleAuthorizationDenied_returns403() {
    AuthorizationDeniedException e = new AuthorizationDeniedException("denied");

    ResponseEntity<ErrorResponse> response = handler.handleAuthorizationDenied(e);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(response.getBody().code()).isEqualTo("FORBIDDEN");
  }

  @Test
  @DisplayName("그 외 예상 못한 예외 - 500으로 최후 안전망 처리된다")
  void handleException_returns500() {
    ResponseEntity<ErrorResponse> response = handler.handleException(new RuntimeException("boom"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
  }
}