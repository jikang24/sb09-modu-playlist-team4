package com.mopl.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.mopl.infra.redis.RedisMessageSubscriber;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.util.ErrorHandler;

@DisplayName("RedisConfig 테스트")
class RedisConfigTest {

  @Test
  @DisplayName("RedisMessageListenerContainer에 에러 핸들러가 등록된다")
  void redisMessageListenerContainer_hasErrorHandler() throws Exception {
    // given
    RedisConfig redisConfig = new RedisConfig();
    RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
    RedisMessageSubscriber subscriber = mock(RedisMessageSubscriber.class);

    // when
    RedisMessageListenerContainer container =
        redisConfig.redisMessageListenerContainer(connectionFactory, subscriber);

    // then
    assertThat(container).isNotNull();

    Field errorHandlerField = findFieldInHierarchy(container.getClass(), "errorHandler");
    errorHandlerField.setAccessible(true);
    Object errorHandler = errorHandlerField.get(container);

    assertThat(errorHandler).isNotNull();
    assertThat(errorHandler).isInstanceOf(ErrorHandler.class);
  }

  /** private 필드가 부모 클래스에 선언돼 있을 수도 있어 클래스 계층을 거슬러 올라가며 찾는다 */
  private Field findFieldInHierarchy(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> current = clazz;
    while (current != null) {
      try {
        return current.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        current = current.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName + " not found in hierarchy of " + clazz);
  }
}
