package com.mopl.domain.watchingsession.adapter.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.watchingsession.dto.WatchingSessionChange;
import com.mopl.domain.watchingsession.dto.WatchingSessionChange.ChangeType;
import com.mopl.global.config.RedisConfig;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("WatchingSessionEventPublisher 테스트")
class WatchingSessionEventPublisherTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  private WatchingSessionEventPublisher publisher;

  @Test
  @DisplayName("고정 채널에 destination/payload 봉투를 JSON으로 발행한다")
  void publish_sendsEnvelopeToFixedChannel() throws Exception {
    publisher = new WatchingSessionEventPublisher(redisTemplate, objectMapper);
    UUID contentId = UUID.randomUUID();
    WatchingSessionChange change = new WatchingSessionChange(ChangeType.JOIN, null, 3L);

    publisher.publish(contentId, change);

    ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
    then(redisTemplate).should().convertAndSend(
        org.mockito.ArgumentMatchers.eq(RedisConfig.WATCHING_SESSION_CHANNEL), payloadCaptor.capture());

    var message = objectMapper.readTree(payloadCaptor.getValue());
    assertThat(message.get("destination").asText()).isEqualTo("contents/" + contentId + "/watch");
    assertThat(message.get("payload").get("type").asText()).isEqualTo("JOIN");
    assertThat(message.get("payload").get("watcherCount").asLong()).isEqualTo(3L);
  }

  @Test
  @DisplayName("직렬화/발행 중 예외가 발생해도 조용히 삼키고 전파하지 않는다")
  void publish_swallowsException() throws Exception {
    ObjectMapper failingMapper = org.mockito.Mockito.mock(ObjectMapper.class);
    given(failingMapper.writeValueAsString(any())).willThrow(new RuntimeException("boom"));
    publisher = new WatchingSessionEventPublisher(redisTemplate, failingMapper);
    UUID contentId = UUID.randomUUID();
    WatchingSessionChange change = new WatchingSessionChange(ChangeType.LEAVE, null, 0L);

    publisher.publish(contentId, change);

    then(redisTemplate).should(never()).convertAndSend(anyString(), anyString());
  }
}