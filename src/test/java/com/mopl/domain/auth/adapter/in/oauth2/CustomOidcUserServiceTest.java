package com.mopl.domain.auth.adapter.in.oauth2;

import com.mopl.domain.auth.port.out.user.RegisterSocialUserPort;
import com.mopl.domain.user.dto.SocialProvider;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomOidcUserService 테스트")
class CustomOidcUserServiceTest {

    @Mock
    private RegisterSocialUserPort registerSocialUserPort;

    private CustomOidcUserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOidcUserService(registerSocialUserPort);
    }

    private OidcUserRequest googleUserRequest(Map<String, Object> claims) {
        // userInfoUri를 비워두면 OidcUserService가 실제 네트워크 호출 없이 ID 토큰의 클레임만으로 사용자를 구성한다.
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userNameAttributeName("sub")
                .clientName("Google")
                .scope("openid", "profile", "email")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token-value",
                Instant.now(), Instant.now().plusSeconds(3600));

        OidcIdToken idToken = new OidcIdToken(
                "id-token-value", Instant.now(), Instant.now().plusSeconds(3600), claims);

        return new OidcUserRequest(registration, accessToken, idToken);
    }

    @Test
    @DisplayName("성공: 구글 로그인 시 사용자를 등록/연동하고 PrincipalOidcUser를 반환한다")
    void loadUser_google_success() {
        Map<String, Object> claims = Map.of(
                "sub", "google-sub-1",
                "email", "woody@gmail.com",
                "name", "woody",
                "picture", "https://example.com/pic.png",
                "email_verified", true
        );
        OidcUserRequest userRequest = googleUserRequest(claims);
        UUID resolvedUserId = UUID.randomUUID();
        when(registerSocialUserPort.registerOrLink(
                SocialProvider.GOOGLE, "google-sub-1", "woody@gmail.com", true, "woody", "https://example.com/pic.png"))
                .thenReturn(resolvedUserId);

        OidcUser result = service.loadUser(userRequest);

        assertThat(result).isInstanceOf(PrincipalOidcUser.class);
        assertThat(((PrincipalOidcUser) result).getUserId()).isEqualTo(resolvedUserId);
    }

    @Test
    @DisplayName("실패: 계정 연동 중 MoplException이 발생하면 OAuth2AuthenticationException으로 변환한다")
    void loadUser_registerFails_throwsOAuth2AuthenticationException() {
        Map<String, Object> claims = Map.of("sub", "google-sub-2", "email", "buzz@gmail.com");
        OidcUserRequest userRequest = googleUserRequest(claims);
        when(registerSocialUserPort.registerOrLink(any(), any(), any(), any(Boolean.class), any(), any()))
                .thenThrow(new MoplException(ErrorCode.DUPLICATE_EMAIL));

        assertThatThrownBy(() -> service.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_EMAIL.name()));
    }
}
