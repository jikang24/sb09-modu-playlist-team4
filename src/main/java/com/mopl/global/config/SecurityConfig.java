package com.mopl.global.config;

import com.mopl.domain.auth.adapter.in.oauth2.CustomOAuth2UserService;
import com.mopl.domain.auth.adapter.in.oauth2.CustomOidcUserService;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.auth.UserAuthPort;
import com.mopl.global.jwt.JwtAuthenticationFilter;
import com.mopl.global.jwt.JwtProperties;
import com.mopl.global.security.MoplAuthenticationProvider;
import com.mopl.global.security.csrf.CsrfCookieFilter;
import com.mopl.global.security.filter.JsonLoginFilter;
import com.mopl.global.security.handler.MoplLoginFailureHandler;
import com.mopl.global.security.handler.MoplLoginSuccessHandler;
import com.mopl.global.security.handler.MoplLogoutHandler;
import com.mopl.global.security.handler.MoplLogoutSuccessHandler;
import com.mopl.global.security.handler.MoplAccessDeniedHandler;
import com.mopl.global.security.handler.MoplAuthenticationEntryPoint;
import com.mopl.global.security.handler.MoplOAuth2LoginFailureHandler;
import com.mopl.global.security.handler.MoplOAuth2LoginSuccessHandler;
import com.mopl.global.security.oauth2.CookieOAuth2AuthorizationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CsrfCookieFilter csrfCookieFilter;
    private final MoplAuthenticationEntryPoint authenticationEntryPoint;
    private final MoplAccessDeniedHandler accessDeniedHandler;
    private final PasswordResetTokenPort passwordResetTokenPort;
    private final MoplLoginSuccessHandler loginSuccessHandler;
    private final MoplLoginFailureHandler loginFailureHandler;
    private final MoplLogoutHandler logoutHandler;
    private final MoplLogoutSuccessHandler logoutSuccessHandler;
    private final UserAuthPort userAuthPort;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final MoplOAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final MoplOAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final CookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${security.csrf.disabled:false}")
    private boolean csrfDisabled;

    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    // 공통 정적 리소스 경로
    private static final String[] PUBLIC_RESOURCES = {
            "/", "/index.html", "/favicon.ico", "/static/**", "/assets/**",
            "/*.js", "/*.css", "/*.png", "/*.svg", "/*.jpg", "/*.jpeg", "/*.gif", "/uploads/**" };

    // 공통 오픈 API 경로
    private static final String[] PUBLIC_APIS = {
            "/api/auth/**", "/ws/**", "/oauth2/authorization/**", "/login/oauth2/code/**","/actuator/**"};

    // Swagger 문서 경로
    private static final String[] SWAGGER_URLS = {
            "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
            "/swagger-resources/**", "/webjars/**"};

    // CSRF 검증에서 완전히 제외할 경로들
    private static final String[] CSRF_IGNORING_URLS = {
            "/ws/**", "/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authenticationManager,
                                           MoplAuthenticationProvider authenticationProvider) throws Exception {
        PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();

        JsonLoginFilter jsonAuthenticationFilter = new JsonLoginFilter(authenticationManager);
        jsonAuthenticationFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
        jsonAuthenticationFilter.setAuthenticationFailureHandler(loginFailureHandler);

        http.formLogin(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProvider)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        // CSRF 설정
        if (csrfDisabled) {
            http.csrf(AbstractHttpConfigurer::disable);
        } else {
            http.csrf(csrf -> {
                CookieCsrfTokenRepository csrfRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
                csrfRepo.setCookieName("XSRF-TOKEN");
                csrfRepo.setHeaderName("X-XSRF-TOKEN");
                csrfRepo.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite("Lax"));

                CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName(null);

                csrf.csrfTokenRepository(csrfRepo)
                        .csrfTokenRequestHandler(requestHandler)
                        .ignoringRequestMatchers(CSRF_IGNORING_URLS)
                        .ignoringRequestMatchers(path.matcher(HttpMethod.POST, "/api/users"));
            });
        }

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_RESOURCES).permitAll()
                .requestMatchers(PUBLIC_APIS).permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(SWAGGER_URLS).permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated());

        http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                        .authorizationRequestRepository(authorizationRequestRepository))
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService)
                        .oidcUserService(customOidcUserService))
                .successHandler(oAuth2LoginSuccessHandler)
                .failureHandler(oAuth2LoginFailureHandler));

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler));

        http.logout(logout -> logout
                .logoutUrl("/api/auth/sign-out")
                .addLogoutHandler(logoutHandler)
                .logoutSuccessHandler(logoutSuccessHandler)
                .clearAuthentication(true));

        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, CsrfFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public MoplAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        return new MoplAuthenticationProvider(userAuthPort, passwordResetTokenPort, passwordEncoder);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}