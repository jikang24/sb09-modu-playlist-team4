package com.mopl;

import com.mopl.global.jwt.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync  // @Async 이벤트 리스너 활성화
@EnableConfigurationProperties(JwtProperties.class)  // JwtProperties 빈 등록
public class MoplApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoplApplication.class, args);
    }
}
