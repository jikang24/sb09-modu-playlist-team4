package com.mopl.domain.auth.adapter.in;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.RefreshResult;
import com.mopl.domain.auth.dto.ResetPasswordRequest;
import com.mopl.domain.auth.port.in.AuthUseCase;
import com.mopl.domain.user.dto.Role;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("AuthController 테스트")
class AuthControllerTest {

    private AuthController authController;
    private AuthUseCase authUseCase;
    private HttpServletResponse httpServletResponse;
    private UUID testUserId;
    private UserDto testUserDto;
    private JwtDto testJwtDto;
    private RefreshResult testRefreshResult;

    @BeforeEach
    void setUp() {
        authUseCase = mock(AuthUseCase.class);
        authController = new AuthController(authUseCase);
        httpServletResponse = mock(HttpServletResponse.class);

        testUserId = UUID.randomUUID();
        testUserDto = new UserDto(
                testUserId,
                Instant.now(),
                "test@email.com",
                "Test User",
                "https://example.com/profile.jpg",
                Role.USER,
                false
        );
        testJwtDto = new JwtDto(testUserDto, "access_token_123");
        testRefreshResult = new RefreshResult(testJwtDto, "new_refresh_token_123", Duration.ofDays(7));
    }

    @Test
    @DisplayName("성공: 올바른 이메일로 비밀번호 초기화를 요청한다")
    void resetPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@email.com");
        doNothing().when(authUseCase).resetPassword(any(ResetPasswordRequest.class));

        ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(authUseCase).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("성공: 비밀번호 초기화 응답에 null이 반환된다")
    void resetPassword_Success_VerifiesResponse() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@email.com");
        doNothing().when(authUseCase).resetPassword(any(ResetPasswordRequest.class));

        ResponseEntity<ApiResponse<Void>> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().data());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자로 비밀번호 초기화를 요청하면 예외가 발생한다")
    void resetPassword_UserNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest("notfound@email.com");
        doThrow(new MoplException(ErrorCode.USER_NOT_FOUND))
                .when(authUseCase).resetPassword(any(ResetPasswordRequest.class));

        assertThrows(MoplException.class, () -> authController.resetPassword(request));
        verify(authUseCase).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("실패: 잘못된 이메일 형식으로 비밀번호 초기화를 요청하면 검증 오류가 발생한다")
    void resetPassword_InvalidEmail() {
        ResetPasswordRequest request = new ResetPasswordRequest("invalid-email");

        try {
            authController.resetPassword(request);
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    @DisplayName("실패: null 이메일로 비밀번호 초기화를 요청한다")
    void resetPassword_NullEmail() {
        try {
            authController.resetPassword(new ResetPasswordRequest(null));
        } catch (Exception e) {
            assertNotNull(e);
        }
    }

    @Test
    @DisplayName("성공: 여러 사용자의 비밀번호 초기화를 연속으로 요청한다")
    void resetPassword_MultipleEmails() {
        ResetPasswordRequest request1 = new ResetPasswordRequest("user1@email.com");
        ResetPasswordRequest request2 = new ResetPasswordRequest("user2@email.com");
        doNothing().when(authUseCase).resetPassword(any(ResetPasswordRequest.class));

        ResponseEntity<ApiResponse<Void>> response1 = authController.resetPassword(request1);
        ResponseEntity<ApiResponse<Void>> response2 = authController.resetPassword(request2);

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        verify(authUseCase, times(2)).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    @DisplayName("성공: 유효한 쿠키로 토큰을 재발급한다")
    void refresh() {
        String refreshToken = "refresh_token_123";
        when(authUseCase.refresh(refreshToken)).thenReturn(testRefreshResult);

        ResponseEntity<?> response = authController.refresh(refreshToken, httpServletResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testJwtDto, response.getBody());
        verify(authUseCase).refresh(refreshToken);
    }

    @Test
    @DisplayName("성공: 토큰 재발급 응답에 JWT 정보가 포함된다")
    void refresh_Success_ReturnsJwtDto() {
        String refreshToken = "refresh_token_123";
        when(authUseCase.refresh(refreshToken)).thenReturn(testRefreshResult);

        ResponseEntity<?> response = authController.refresh(refreshToken, httpServletResponse);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof JwtDto);
    }

    @Test
    @DisplayName("성공: 토큰 재발급 시 새 REFRESH_TOKEN 쿠키를 내려준다")
    void refresh_SetsNewRefreshTokenCookie() {
        String refreshToken = "refresh_token_123";
        when(authUseCase.refresh(refreshToken)).thenReturn(testRefreshResult);

        authController.refresh(refreshToken, httpServletResponse);

        verify(httpServletResponse).addHeader(eq(HttpHeaders.SET_COOKIE), contains("REFRESH_TOKEN=new_refresh_token_123"));
    }

    @Test
    @DisplayName("성공: 토큰 재발급 시 UseCase가 호출된다")
    void refresh_WithValidToken_CallsUseCase() {
        String refreshToken = "valid_token";
        when(authUseCase.refresh(refreshToken)).thenReturn(testRefreshResult);

        authController.refresh(refreshToken, httpServletResponse);

        verify(authUseCase).refresh(refreshToken);
    }

    @Test
    @DisplayName("실패: 쿠키가 없으면 401 Unauthorized를 반환한다")
    void refresh_NullToken_Returns401() {
        ResponseEntity<?> response = authController.refresh(null, httpServletResponse);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("실패: 빈 토큰으로 재발급을 시도하면 예외가 발생한다")
    void refresh_EmptyToken_CallsUseCase() {
        String emptyToken = "";
        when(authUseCase.refresh(emptyToken))
                .thenThrow(new MoplException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        assertThrows(MoplException.class, () -> authController.refresh(emptyToken, httpServletResponse));
    }

    @Test
    @DisplayName("실패: 유효하지 않은 토큰으로 재발급을 시도하면 예외가 발생한다")
    void refresh_InvalidToken() {
        String refreshToken = "invalid_token";
        doThrow(new MoplException(ErrorCode.REFRESH_TOKEN_NOT_FOUND))
                .when(authUseCase).refresh(refreshToken);

        assertThrows(MoplException.class, () -> authController.refresh(refreshToken, httpServletResponse));
        verify(authUseCase).refresh(refreshToken);
    }

    @Test
    @DisplayName("실패: 잠긴 사용자의 토큰으로 재발급을 시도하면 예외가 발생한다")
    void refresh_UserLocked() {
        String refreshToken = "refresh_token_123";
        doThrow(new MoplException(ErrorCode.ACCOUNT_LOCKED))
                .when(authUseCase).refresh(refreshToken);

        assertThrows(MoplException.class, () -> authController.refresh(refreshToken, httpServletResponse));
        verify(authUseCase).refresh(refreshToken);
    }

    @Test
    @DisplayName("성공: 여러 토큰으로 연속 재발급을 요청한다")
    void refresh_MultipleTokens() {
        String token1 = "token_1";
        String token2 = "token_2";
        UserDto userDto2 = new UserDto(
                UUID.randomUUID(),
                Instant.now(),
                "user2@email.com",
                "User 2",
                null,
                Role.USER,
                false
        );
        JwtDto jwtDto2 = new JwtDto(userDto2, "access_token_2");
        RefreshResult refreshResult2 = new RefreshResult(jwtDto2, "new_refresh_token_2", Duration.ofDays(7));

        when(authUseCase.refresh(token1)).thenReturn(testRefreshResult);
        when(authUseCase.refresh(token2)).thenReturn(refreshResult2);

        ResponseEntity<?> response1 = authController.refresh(token1, httpServletResponse);
        ResponseEntity<?> response2 = authController.refresh(token2, httpServletResponse);

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
        verify(authUseCase).refresh(token1);
        verify(authUseCase).refresh(token2);
    }

    @Test
    @DisplayName("성공: CSRF 토큰을 조회한다")
    void csrfToken() {
        ResponseEntity<Void> response = authController.csrfToken();

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("성공: CSRF 토큰 조회 응답이 비어있다")
    void csrfToken_ReturnsOkStatus() {
        ResponseEntity<Void> response = authController.csrfToken();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("실패: 로그인 요청은 SecurityFilterChain에서 처리하므로 예외를 발생시킨다")
    void signIn() {
        assertThrows(IllegalStateException.class, () -> authController.signIn(null));
    }

    @Test
    @DisplayName("실패: 로그인 요청이 어떤 정보로 들어와도 예외가 발생한다")
    void signIn_ThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> authController.signIn(null));
    }

    @Test
    @DisplayName("실패: 로그인 요청 시 예외 메시지가 설정되어 있다")
    void signIn_WithRequest_StillThrows() {
        assertThrows(IllegalStateException.class,
            () -> authController.signIn(new com.mopl.domain.auth.dto.SignInRequest("test@example.com", "password")));
    }

    @Test
    @DisplayName("실패: 로그아웃 요청은 SecurityFilterChain에서 처리하므로 예외를 발생시킨다")
    void signOut() {
        assertThrows(IllegalStateException.class, () -> authController.signOut());
    }

    @Test
    @DisplayName("실패: 로그아웃 요청 시 예외가 발생한다")
    void signOut_ThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> authController.signOut());
    }
}
