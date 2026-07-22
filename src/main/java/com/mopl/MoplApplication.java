package com.mopl;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class MoplApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoplApplication.class, args);
    }

    @Bean
    ApplicationRunner txRunner(ApplicationContext context) {
        return args -> {
            System.out.println("===== Transaction Managers =====");
            context.getBeansOfType(PlatformTransactionManager.class)
                .forEach((name, tm) ->
                    System.out.println(name + " -> " + tm.getClass().getName()));
        };
    }
}