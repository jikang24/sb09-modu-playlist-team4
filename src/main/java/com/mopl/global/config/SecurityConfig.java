package com.mopl.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

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
    private final ObjectMapper objectMapper;
    private final MoplLoginSuccessHandler loginSuccessHandler;
    private final MoplLoginFailureHandler loginFailureHandler;
    private final MoplLogoutHandler logoutHandler;
    private final MoplLogoutSuccessHandler logoutSuccessHandler;
    private final UserAuthPort userAuthPort;

    @Value("${security.csrf.disabled:false}")
    private boolean csrfDisabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authenticationManager) throws Exception {
        PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();

        // JSON 로그인 필터 설정
        JsonLoginFilter jsonAuthenticationFilter = new JsonLoginFilter(
                authenticationManager, objectMapper);
        jsonAuthenticationFilter.setAuthenticationSuccessHandler(loginSuccessHandler);
        jsonAuthenticationFilter.setAuthenticationFailureHandler(loginFailureHandler);


        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .csrf(csrf -> {
                    if (csrfDisabled) {
                        csrf.disable();
                    } else {
                        csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                                .ignoringRequestMatchers("/ws/**", "/api/auth/**")
                                .ignoringRequestMatchers(path.matcher(HttpMethod.POST, "/api/users"));
                    }
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/sse/**", "/ws/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/swagger-resources/**", "/webjars/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())

                .logout(logout -> logout
                        .logoutUrl("/api/auth/sign-out")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(false)
                        .clearAuthentication(true))

                .addFilterBefore(jsonAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, BasicAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public MoplAuthenticationProvider authenticationProvider() {
        return new MoplAuthenticationProvider(userAuthPort, passwordResetTokenPort, passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
