package com.mopl.domain.conversation.adapter.in.web.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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

  @Test
  @DisplayName("목록 조회: 대화 여러 건을 벌크 조회로 한 번에 조립한다")
  void toDtoList_success_bulkAssembly() {

    UUID myId = UUID.randomUUID();
    UUID otherId1 = UUID.randomUUID();
    UUID otherId2 = UUID.randomUUID();
    UUID conversationId1 = UUID.randomUUID();
    UUID conversationId2 = UUID.randomUUID();
    UUID dmId = UUID.randomUUID();

    Conversation conversation1 = Mockito.mock(Conversation.class);
    given(conversation1.getId()).willReturn(conversationId1);
    given(conversation1.getOtherParticipant(myId)).willReturn(otherId1);

    Conversation conversation2 = Mockito.mock(Conversation.class);
    given(conversation2.getId()).willReturn(conversationId2);
    given(conversation2.getOtherParticipant(myId)).willReturn(otherId2);

    UserSummary otherSummary1 = new UserSummary(otherId1, "상대1", null);
    UserSummary otherSummary2 = new UserSummary(otherId2, "상대2", null);
    UserSummary mySummary = new UserSummary(myId, "나", null);

    DirectMessage latestForConv1 = Mockito.mock(DirectMessage.class);
    given(latestForConv1.getId()).willReturn(dmId);
    given(latestForConv1.getConversationId()).willReturn(conversationId1);
    given(latestForConv1.getSenderId()).willReturn(myId);
    given(latestForConv1.getReceiverId()).willReturn(otherId1);
    given(latestForConv1.getContent()).willReturn("안녕하세요");
    given(latestForConv1.getCreatedAt()).willReturn(Instant.now());

    given(getLatestDirectMessageUseCase.getLatestBulk(List.of(conversationId1, conversationId2)))
        .willReturn(Map.of(conversationId1, latestForConv1));
    given(loadUserPort.getUserSummaries(anyCollection()))
        .willReturn(Map.of(otherId1, otherSummary1, otherId2, otherSummary2, myId, mySummary));
    given(checkUnreadDirectMessageUseCase.hasUnreadBulk(List.of(conversationId1, conversationId2), myId))
        .willReturn(Set.of(conversationId1));


    List<ConversationDto> result =
        conversationWebMapper.toDtoList(List.of(conversation1, conversation2), myId);


    assertThat(result).hasSize(2);

    ConversationDto dto1 = result.get(0);
    assertThat(dto1.id()).isEqualTo(conversationId1);
    assertThat(dto1.with()).isEqualTo(otherSummary1);
    assertThat(dto1.hasUnread()).isTrue();
    assertThat(dto1.lastestMessage()).isNotNull();
    assertThat(dto1.lastestMessage().content()).isEqualTo("안녕하세요");
    assertThat(dto1.lastestMessage().sender()).isEqualTo(mySummary);
    assertThat(dto1.lastestMessage().receiver()).isEqualTo(otherSummary1);

    ConversationDto dto2 = result.get(1);
    assertThat(dto2.id()).isEqualTo(conversationId2);
    assertThat(dto2.with()).isEqualTo(otherSummary2);
    assertThat(dto2.hasUnread()).isFalse();
    assertThat(dto2.lastestMessage()).isNull();
  }

  @Test
  @DisplayName("목록 조회: 건별 개별 조회 메서드가 아니라 벌크 메서드만 각각 한 번씩 호출한다 (N+1 회귀 방지)")
  void toDtoList_usesBulkCallsOnly_notPerItem() {

    UUID myId = UUID.randomUUID();
    UUID conversationId1 = UUID.randomUUID();
    UUID conversationId2 = UUID.randomUUID();

    Conversation conversation1 = Mockito.mock(Conversation.class);
    given(conversation1.getId()).willReturn(conversationId1);
    given(conversation1.getOtherParticipant(myId)).willReturn(UUID.randomUUID());

    Conversation conversation2 = Mockito.mock(Conversation.class);
    given(conversation2.getId()).willReturn(conversationId2);
    given(conversation2.getOtherParticipant(myId)).willReturn(UUID.randomUUID());

    given(getLatestDirectMessageUseCase.getLatestBulk(anyCollection())).willReturn(Map.of());
    given(loadUserPort.getUserSummaries(anyCollection())).willReturn(Map.of());
    given(checkUnreadDirectMessageUseCase.hasUnreadBulk(anyCollection(), any()))
        .willReturn(Set.of());


    conversationWebMapper.toDtoList(List.of(conversation1, conversation2), myId);


    verify(loadUserPort, times(1)).getUserSummaries(anyCollection());
    verify(getLatestDirectMessageUseCase, times(1)).getLatestBulk(anyCollection());
    verify(checkUnreadDirectMessageUseCase, times(1)).hasUnreadBulk(anyCollection(), any());

    then(loadUserPort).should(never()).getUserSummary(any());
    then(getLatestDirectMessageUseCase).should(never()).getLatest(any());
    then(checkUnreadDirectMessageUseCase).should(never()).hasUnread(any(), any());
  }

  @Test
  @DisplayName("목록이 비어있으면 어떤 포트도 호출하지 않고 빈 리스트를 반환한다")
  void toDtoList_emptyList_skipsAllPortCalls() {

    UUID myId = UUID.randomUUID();

    List<ConversationDto> result = conversationWebMapper.toDtoList(List.of(), myId);

    assertThat(result).isEmpty();
    then(loadUserPort).should(never()).getUserSummaries(any());
    then(getLatestDirectMessageUseCase).should(never()).getLatestBulk(any());
    then(checkUnreadDirectMessageUseCase).should(never()).hasUnreadBulk(any(), any());
  }
}