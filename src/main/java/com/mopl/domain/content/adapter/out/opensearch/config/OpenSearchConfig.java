package com.mopl.domain.content.adapter.out.opensearch.config;

import java.net.URI;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

@Configuration
public class OpenSearchConfig {

  @Value("${opensearch.uri}")
  private String uri;

  @Value("${opensearch.username:}")
  private String username;

  @Value("${opensearch.password:}")
  private String password;

  @Bean
  public OpenSearchClient openSearchClient() {
    URI parsed = URI.create(uri);
    HttpHost httpHost = new HttpHost(parsed.getScheme(), parsed.getHost(), parsed.getPort());

    boolean useAuth = StringUtils.hasText(username) && StringUtils.hasText(password);

    OpenSearchTransport transport =
        ApacheHttpClient5TransportBuilder
            .builder(httpHost)
            .setHttpClientConfigCallback(httpClientBuilder -> {
              PoolingAsyncClientConnectionManager connectionManager =
                  PoolingAsyncClientConnectionManagerBuilder.create()
                      .setMaxConnTotal(100)
                      .setMaxConnPerRoute(100)
                      .build();
              httpClientBuilder.setConnectionManager(connectionManager);

              RequestConfig requestConfig = RequestConfig.custom()
                  .setConnectTimeout(Timeout.ofSeconds(2))
                  .setResponseTimeout(Timeout.ofSeconds(3))
                  .build();

              httpClientBuilder
                  .setConnectionManager(connectionManager)
                  .setDefaultRequestConfig(requestConfig);

              if (useAuth) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    new AuthScope(httpHost),
                    new UsernamePasswordCredentials(username, password.toCharArray()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
              }

              return httpClientBuilder;
            })
            .build();

    return new OpenSearchClient(transport);
  }
}