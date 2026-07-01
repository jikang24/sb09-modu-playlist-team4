package com.mopl.infra.tmdb;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "mopl.tmdb")
public class TmdbProperties {

  private final String accessToken;
  private final String baseUrl;
  private final String imageBaseUrl;

  public TmdbProperties(String accessToken, String baseUrl, String imageBaseUrl) {
    this.accessToken = accessToken;
    this.baseUrl = baseUrl;
    this.imageBaseUrl = imageBaseUrl;
  }
}
