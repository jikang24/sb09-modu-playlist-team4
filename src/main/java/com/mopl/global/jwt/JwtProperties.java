package com.mopl.global.jwt;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@ConfigurationProperties(prefix = "mopl.jwt")
public class JwtProperties {

    private final String secret;
    private final long accessTokenExpiryMs;
    private final long refreshTokenExpiryMs;

    public JwtProperties(String secret, long accessTokenExpiryMs, long refreshTokenExpiryMs) {
        this.secret = secret;
        this.accessTokenExpiryMs = accessTokenExpiryMs;
        this.refreshTokenExpiryMs = refreshTokenExpiryMs;
    }
}