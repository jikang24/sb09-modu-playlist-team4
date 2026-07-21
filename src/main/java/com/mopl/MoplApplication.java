package com.mopl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync  // @Async 이벤트 리스너 활성화
@EnableScheduling // @Scheduled 배치/토큰 정리 작업 활성화
@ConfigurationPropertiesScan //외부 API 빈 등록 어노테이션
public class MoplApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoplApplication.class, args);
    }
}
