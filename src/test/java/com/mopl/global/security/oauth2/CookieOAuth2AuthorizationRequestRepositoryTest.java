package com.mopl.global.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CookieOAuth2AuthorizationRequestRepository 테스트")
class CookieOAuth2AuthorizationRequestRepositoryTest {

    private CookieOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new CookieOAuth2AuthorizationRequestRepository();
        ReflectionTestUtils.setField(repository, "cookieSecure", false);
    }

    private OAuth2AuthorizationRequest sampleRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("client-id")
                .redirectUri("https://mopl.io/login/oauth2/code/google")
                .scopes(java.util.Set.of("openid", "email"))
                .state("state-value")
                .authorizationRequestUri("https://accounts.google.com/o/oauth2/v2/auth?client_id=client-id")
                .build();
    }

    @Test
    @DisplayName("성공: 저장한 뒤 쿠키에서 authorizationRequest를 다시 읽어온다")
    void saveAndLoad_roundTrip() {
        OAuth2AuthorizationRequest authRequest = sampleRequest();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authRequest, request, response);

        jakarta.servlet.http.Cookie cookie = response.getCookie(
                CookieOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();

        MockHttpServletRequest loadRequest = new MockHttpServletRequest();
        loadRequest.setCookies(cookie);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(loadRequest);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getState()).isEqualTo("state-value");
        assertThat(loaded.getClientId()).isEqualTo("client-id");
    }

    @Test
    @DisplayName("성공: authorizationRequest가 null이면 쿠키를 삭제한다")
    void saveNull_deletesCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(null, request, response);

        jakarta.servlet.http.Cookie cookie = response.getCookie(
                CookieOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("실패: 쿠키가 없으면 null을 반환한다")
    void loadAuthorizationRequest_noCookie_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNull();
    }

    @Test
    @DisplayName("성공: removeAuthorizationRequest는 값을 반환하고 쿠키를 삭제한다")
    void removeAuthorizationRequest_returnsAndDeletes() {
        OAuth2AuthorizationRequest authRequest = sampleRequest();
        MockHttpServletRequest saveRequest = new MockHttpServletRequest();
        MockHttpServletResponse saveResponse = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authRequest, saveRequest, saveResponse);
        jakarta.servlet.http.Cookie savedCookie = saveResponse.getCookie(
                CookieOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME);

        MockHttpServletRequest removeRequest = new MockHttpServletRequest();
        removeRequest.setCookies(savedCookie);
        MockHttpServletResponse removeResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(removeRequest, removeResponse);

        assertThat(removed).isNotNull();
        assertThat(removed.getState()).isEqualTo("state-value");
        jakarta.servlet.http.Cookie deletedCookie = removeResponse.getCookie(
                CookieOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME);
        assertThat(deletedCookie.getMaxAge()).isZero();
    }

    @Test
    @DisplayName("실패: 쿠키 값이 손상되어 있으면 역직렬화 실패로 null을 반환한다")
    void loadAuthorizationRequest_corruptedCookie_returnsNull() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie(
                CookieOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST_COOKIE_NAME, "not-valid-base64!!"));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isNull();
    }
}
