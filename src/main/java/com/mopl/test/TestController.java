package com.mopl.test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
    kafkaTemplate.send("mopl_topic", "Hello ECS");
    return "Kafka Message Sent";
  }

  @GetMapping("/test/opensearch")
  public String opensearch() {
    RestTemplate restTemplate = new RestTemplate();

    // 마스터 계정 정보 적용
    String auth = "mopl-master:Mopl1234!";
    String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Basic " + encodedAuth);

    HttpEntity<String> entity = new HttpEntity<>(headers);

    // openSearchUri는 환경변수로 받아오신 엔드포인트가 들어갑니다.
    ResponseEntity<String> response = restTemplate.exchange(openSearchUri, HttpMethod.GET, entity, String.class);

    return response.getBody();
  }
}