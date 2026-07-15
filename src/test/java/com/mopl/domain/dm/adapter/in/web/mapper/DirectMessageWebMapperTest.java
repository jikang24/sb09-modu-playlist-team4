package com.mopl.domain.dm.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.mopl.domain.dm.application.port.out.LoadUserPort;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectMessageWebMapperTest {

  @Mock
  private LoadUserPort loadUserPort;

  @InjectMocks
  private DirectMessageWebMapper mapper;

  @Test
  @DisplayName("DirectMessage를 DTO로 변환한다")
  void toDto_success() {

    UUID senderId = UUID.randomUUID();
    UUID receiverId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();

    DirectMessage dm = new DirectMessage(
        UUID.randomUUID(),
        conversationId,
        senderId,
        receiverId,
        "안녕하세요",
        Instant.now(),
        false
    );

    UserSummary sender = new UserSummary(senderId, "sender", null);
    UserSummary receiver = new UserSummary(receiverId, "receiver", null);

    when(loadUserPort.getUserSummary(senderId)).thenReturn(sender);
    when(loadUserPort.getUserSummary(receiverId)).thenReturn(receiver);

    DirectMessageDto dto = mapper.toDto(dm);

    assertThat(dto.id()).isEqualTo(dm.getId());
    assertThat(dto.conversationId()).isEqualTo(conversationId);
    assertThat(dto.sender()).isEqualTo(sender);
    assertThat(dto.receiver()).isEqualTo(receiver);
    assertThat(dto.content()).isEqualTo("안녕하세요");
  }
}