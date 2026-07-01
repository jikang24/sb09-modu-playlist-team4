package com.mopl.domain.auth.adapter.in;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.ResetPasswordRequest;
import com.mopl.domain.auth.dto.SignInRequest;
import com.mopl.domain.auth.port.in.AuthUseCase;
import com.mopl.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증 관리")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;

    @Operation(summary = "비밀번호 초기화", description = "임시 비밀번호로 초기화 후 이메일로 전송합니다.")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authUseCase.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @CookieValue(value = "REFRESH_TOKEN", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        JwtDto result = authUseCase.refresh(refreshToken);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "CSRF 토큰 조회", description = "CSRF 토큰을 조회합니다. 토큰은 쿠키(XSRF-TOKEN)에 저장됩니다.")
    @GetMapping("/csrf-token")
    public ResponseEntity<Void> csrfToken() {
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "로그인", description = "SecurityFilterChain에서 처리합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = JwtDto.class)))
    @PostMapping("/sign-in")
    public JwtDto signIn(@RequestBody SignInRequest request) {
        throw new IllegalStateException("Should be handled by SecurityFilterChain");
    }

    @Operation(summary = "로그아웃", description = "SecurityFilterChain에서 처리합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/sign-out")
    public void signOut() {
        throw new IllegalStateException("Should be handled by SecurityFilterChain");
    }
}
