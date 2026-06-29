package com.mopl.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String securityJwtName = "JWT_Auth";

        // 1. 모든 API 요청 시 헤더에 토큰이 포함되도록 기본 요구사항 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securityJwtName);

        // 2. Swagger UI에서 토큰을 입력할 수 있는 자물쇠(Authorize) 버튼 구성
        Components components = new Components().addSecuritySchemes(securityJwtName,
                new SecurityScheme()
                        .name(securityJwtName)
                        .type(SecurityScheme.Type.HTTP) // HTTP 방식
                        .scheme("bearer")               // Bearer 헤더 사용
                        .bearerFormat("JWT"));          // 토큰 형식은 JWT

        return new OpenAPI()
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}