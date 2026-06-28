package com.mopl.test;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequiredArgsConstructor
public class TestController {

  private final JdbcTemplate jdbcTemplate;
  private final StringRedisTemplate redisTemplate;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${spring.elasticsearch.uris}")
  private String openSearchUri;

  @GetMapping("/test/db")
  public String db() {
    return jdbcTemplate.queryForObject("SELECT version()", String.class);
  }

  @GetMapping("/test/redis")
  public String redis() {
    redisTemplate.opsForValue().set("test", "hello");
    return redisTemplate.opsForValue().get("test");
  }

  @GetMapping("/test/kafka")
  public String kafka() {
    kafkaTemplate.send("test-topic", "Hello ECS");
    return "Kafka Message Sent";
  }

  @GetMapping("/test/opensearch")
  public String opensearch() {
    RestTemplate restTemplate = new RestTemplate();
    return restTemplate.getForObject(openSearchUri, String.class);
  }
}