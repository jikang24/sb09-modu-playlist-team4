package com.mopl.domain.conversation.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mopl.domain.conversation.adapter.in.web.dto.ConversationDto;
import com.mopl.domain.conversation.application.port.out.LoadUserPort;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.domain.dm.application.port.in.CheckUnreadDirectMessageUseCase;
import com.mopl.domain.dm.application.port.in.GetLatestDirectMessageUseCase;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationWebMapperTest {

  @InjectMocks
  private ConversationWebMapper conversationWebMapper;

  @Mock
  private LoadUserPort loadUserPort;

  @Mock
  private GetLatestDirectMessageUseCase getLatestDirectMessageUseCase;

  @Mock
  private CheckUnreadDirectMessageUseCase checkUnreadDirectMessageUseCase;

  @Test
  @DisplayName("대화방 도메인 모델을 성공적으로 ConversationDto로 매핑한다 - 최신 메시지가 존재하는 경우")
  void toDto_Success_WithLatestMessage() {
    
    UUID myId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();
    UUID dmId = UUID.randomUUID();

    
    Conversation mockConversation = Mockito.mock(Conversation.class);
    given(mockConversation.getId()).willReturn(conversationId);
    given(mockConversation.getOtherParticipant(myId)).willReturn(otherId);

    
    UserSummary otherUserSummary = new UserSummary(otherId, "상대방닉네임", "profile_url");
    UserSummary mySummary = new UserSummary(myId, "내닉네임", "profile_url_2");

    
    DirectMessage mockDm = Mockito.mock(DirectMessage.class);
    given(mockDm.getId()).willReturn(dmId);
    given(mockDm.getConversationId()).willReturn(conversationId);
    given(mockDm.getSenderId()).willReturn(myId);
    given(mockDm.getReceiverId()).willReturn(otherId);
    given(mockDm.getContent()).willReturn("안녕하세요");
    given(mockDm.getCreatedAt()).willReturn(Instant.now());

    
    given(loadUserPort.getUserSummary(otherId)).willReturn(otherUserSummary);
    given(loadUserPort.getUserSummary(myId)).willReturn(mySummary);
    given(checkUnreadDirectMessageUseCase.hasUnread(conversationId, myId)).willReturn(true);

    
    given(getLatestDirectMessageUseCase.getLatest(conversationId))
        .willReturn(Optional.<DirectMessage>of(mockDm));

    
    ConversationDto result = conversationWebMapper.toDto(mockConversation, myId);

    
    assertThat(result).isNotNull();
    assertThat(result.id()).isEqualTo(conversationId);
    assertThat(result.with()).isEqualTo(otherUserSummary);
    assertThat(result.hasUnread()).isTrue();

    DirectMessageDto actualDm = result.lastestMessage();
    assertThat(actualDm).isNotNull();
    assertThat(actualDm.content()).isEqualTo("안녕하세요");
    assertThat(actualDm.sender()).isEqualTo(mySummary);
    assertThat(actualDm.receiver()).isEqualTo(otherUserSummary);

    
    verify(loadUserPort, times(2)).getUserSummary(otherId);
    verify(loadUserPort, times(1)).getUserSummary(myId);
  }

  @Test
  @DisplayName("최신 메시지가 없는 경우 lastestMessage가 null인 ConversationDto를 반환한다")
  void toDto_Success_WhenLatestMessageEmpty() {
    
    UUID myId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    UUID conversationId = UUID.randomUUID();

    Conversation mockConversation = Mockito.mock(Conversation.class);
    given(mockConversation.getId()).willReturn(conversationId);
    given(mockConversation.getOtherParticipant(myId)).willReturn(otherId);

    UserSummary otherUserSummary = new UserSummary(otherId, "상대방닉네임", "profile_url");

    given(loadUserPort.getUserSummary(otherId)).willReturn(otherUserSummary);
    given(checkUnreadDirectMessageUseCase.hasUnread(conversationId, myId)).willReturn(false);

    
    given(getLatestDirectMessageUseCase.getLatest(conversationId))
        .willReturn(Optional.<DirectMessage>empty());

    
    ConversationDto result = conversationWebMapper.toDto(mockConversation, myId);

    
    assertThat(result).isNotNull();
    assertThat(result.lastestMessage()).isNull();
    assertThat(result.hasUnread()).isFalse();
  }
}