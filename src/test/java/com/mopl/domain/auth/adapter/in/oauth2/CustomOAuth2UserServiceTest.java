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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.client.RestOperations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomOAuth2UserService 테스트")
class CustomOAuth2UserServiceTest {

    @Mock
    private RegisterSocialUserPort registerSocialUserPort;

    @Mock
    private RestOperations restOperations;

    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOAuth2UserService(registerSocialUserPort);
        service.setRestOperations(restOperations);
    }

    private OAuth2UserRequest kakaoUserRequest() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("kakao")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id")
                .clientName("Kakao")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token-value",
                Instant.now(), Instant.now().plusSeconds(3600));

        return new OAuth2UserRequest(registration, accessToken);
    }

    private Map<String, Object> kakaoAttributes() {
        Map<String, Object> profile = new HashMap<>();
        profile.put("nickname", "buzz");
        profile.put("profile_image_url", "https://k.kakaocdn.net/pic.png");

        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("profile", profile);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 123456789L);
        attributes.put("kakao_account", kakaoAccount);
        return attributes;
    }

    @SuppressWarnings("unchecked")
    private void stubUserInfoResponse(Map<String, Object> attributes) {
        when(restOperations.exchange(any(RequestEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(attributes, HttpStatus.OK));
    }

    @Test
    @DisplayName("성공: 카카오 로그인 시 사용자를 등록/연동하고 PrincipalOAuth2User를 반환한다")
    void loadUser_kakao_success() {
        OAuth2UserRequest userRequest = kakaoUserRequest();
        stubUserInfoResponse(kakaoAttributes());
        UUID resolvedUserId = UUID.randomUUID();
        when(registerSocialUserPort.registerOrLink(
                SocialProvider.KAKAO, "123456789", "buzz_123456789@kakao.com",
                false, "buzz", "https://k.kakaocdn.net/pic.png"))
                .thenReturn(resolvedUserId);

        OAuth2User result = service.loadUser(userRequest);

        assertThat(result).isInstanceOf(PrincipalOAuth2User.class);
        assertThat(((PrincipalOAuth2User) result).getUserId()).isEqualTo(resolvedUserId);
        assertThat(result.getAttributes()).containsEntry("id", 123456789L);
    }

    @Test
    @DisplayName("실패: 계정 연동 중 MoplException이 발생하면 OAuth2AuthenticationException으로 변환한다")
    void loadUser_registerFails_throwsOAuth2AuthenticationException() {
        OAuth2UserRequest userRequest = kakaoUserRequest();
        stubUserInfoResponse(kakaoAttributes());
        when(registerSocialUserPort.registerOrLink(any(), any(), any(), any(Boolean.class), any(), any()))
                .thenThrow(new MoplException(ErrorCode.ACCOUNT_LOCKED));

        assertThatThrownBy(() -> service.loadUser(userRequest))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(ErrorCode.ACCOUNT_LOCKED.name()));
    }

    @Test
    @DisplayName("실패: 지원하지 않는 registrationId면 예외가 발생한다")
    void loadUser_unsupportedProvider_throws() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("naver")
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me")
                .userNameAttributeName("id")
                .clientName("Naver")
                .build();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token-value",
                Instant.now(), Instant.now().plusSeconds(3600));
        OAuth2UserRequest userRequest = new OAuth2UserRequest(registration, accessToken);
        stubUserInfoResponse(Map.of("id", "1"));

        assertThatThrownBy(() -> service.loadUser(userRequest))
                .isInstanceOf(MoplException.class);
    }
}
