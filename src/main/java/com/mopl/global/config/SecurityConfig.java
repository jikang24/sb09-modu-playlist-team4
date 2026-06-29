package com.mopl.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.auth.port.out.PasswordResetTokenPort;
import com.mopl.global.jwt.AuthTokenService;
import com.mopl.global.jwt.JwtAuthenticationFilter;
import com.mopl.global.jwt.JwtProperties;
import com.mopl.global.jwt.JwtProvider;
import com.mopl.global.security.MoplAuthenticationProvider;
import com.mopl.global.security.csrf.CsrfCookieFilter;
import com.mopl.global.security.filter.JsonLoginFilter;
import com.mopl.global.security.handler.MoplLoginFailureHandler;
import com.mopl.global.security.handler.MoplLoginSuccessHandler;
import com.mopl.global.security.handler.MoplAccessDeniedHandler;
import com.mopl.global.security.handler.MoplAuthenticationEntryPoint;
import com.mopl.global.security.handler.MoplLogoutHandler;
import com.mopl.global.security.handler.MoplLogoutSuccessHandler;
import com.mopl.global.security.userdetails.MoplUserDetailsService;
import lombok.RequiredArgsConstructor;
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
    private final MoplLogoutHandler logoutHandler;
    private final MoplLogoutSuccessHandler logoutSuccessHandler;
    private final MoplUserDetailsService userDetailsService;
    private final PasswordResetTokenPort passwordResetTokenPort;
    private final JwtProvider jwtProvider;
    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           AuthenticationManager authenticationManager) throws Exception {
        PathPatternRequestMatcher.Builder path = PathPatternRequestMatcher.withDefaults();

        JsonLoginFilter jsonLoginFilter = new JsonLoginFilter(authenticationManager, objectMapper);
        jsonLoginFilter.setAuthenticationSuccessHandler(
                new MoplLoginSuccessHandler(jwtProvider, authTokenService, objectMapper));
        jsonLoginFilter.setAuthenticationFailureHandler(
                new MoplLoginFailureHandler(objectMapper));

        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/ws/**", "/api/auth/**")
                        .ignoringRequestMatchers(path.matcher(HttpMethod.POST, "/api/users")))

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

                .addFilterBefore(jsonLoginFilter, UsernamePasswordAuthenticationFilter.class)
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
        MoplAuthenticationProvider provider = new MoplAuthenticationProvider(passwordResetTokenPort);
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        provider.setHideUserNotFoundExceptions(false);
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
