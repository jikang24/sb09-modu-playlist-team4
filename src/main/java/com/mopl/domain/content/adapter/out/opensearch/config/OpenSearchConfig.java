package com.mopl.domain.content.adapter.out.opensearch.config;

import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

  @Value("${opensearch.host}")
  private String host;

  @Value("${opensearch.port}")
  private int port;

  @Bean
  public OpenSearchClient openSearchClient() {

    HttpHost httpHost = new HttpHost("http", host, port);

    OpenSearchTransport transport =
        ApacheHttpClient5TransportBuilder
            .builder(httpHost)
            .setHttpClientConfigCallback(httpClientBuilder -> {
              PoolingAsyncClientConnectionManager connectionManager =
                  PoolingAsyncClientConnectionManagerBuilder.create()
                      .setMaxConnTotal(100)
                      .setMaxConnPerRoute(100)
                      .build();
              return httpClientBuilder.setConnectionManager(connectionManager);
            })
            .build();

    return new OpenSearchClient(transport);
  }
}