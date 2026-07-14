package com.mopl.global.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * CachingConfigurer는 cacheManager()/errorHandler() 등의 메서드 이름/시그니처가 인터페이스와 정확히 일치해야
 * Spring이 캐싱 aspect에 연결하는 유일한 통로가 된다
 * 그렇지 않으면 조용히 기본값(SimpleCacheErrorHandler 등)이 쓰인다.
 */
@Configuration
@EnableCaching
@Slf4j
@RequiredArgsConstructor
public class CacheConfig implements CachingConfigurer {

  private final RedisConnectionFactory connectionFactory;

  @Bean
  @Override
  public CacheManager cacheManager() {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofMinutes(30))
        .serializeKeysWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new StringRedisSerializer()))
        .serializeValuesWith(RedisSerializationContext.SerializationPair
            .fromSerializer(new GenericJackson2JsonRedisSerializer()))
        .disableCachingNullValues();

    Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
    cacheConfigs.put("userSummary", defaultConfig.entryTtl(Duration.ofHours(1)));
    cacheConfigs.put("content", defaultConfig.entryTtl(Duration.ofMinutes(30)));
    cacheConfigs.put("playlist", defaultConfig.entryTtl(Duration.ofMinutes(10)));

    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(defaultConfig)
        .withInitialCacheConfigurations(cacheConfigs)
        .transactionAware()
        .build();
  }

  @Bean
  @Override
  public CacheErrorHandler errorHandler() {
    return new CacheErrorHandler() {
      @Override
      public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
        log.warn("캐시 조회 실패 - cache: {}, key: {}", cache.getName(), key, exception);
      }

      @Override
      public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
        log.warn("캐시 저장 실패 - cache: {}, key: {}", cache.getName(), key, exception);
      }

      @Override
      public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
        log.warn("캐시 삭제 실패 - cache: {}, key: {}", cache.getName(), key, exception);
      }

      @Override
      public void handleCacheClearError(RuntimeException exception, Cache cache) {
        log.warn("캐시 전체 삭제 실패 - cache: {}", cache.getName(), exception);
      }
    };
  }
}