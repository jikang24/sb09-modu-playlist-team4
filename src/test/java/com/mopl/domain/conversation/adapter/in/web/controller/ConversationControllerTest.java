package com.mopl.domain.conversation.adapter.in.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.domain.conversation.adapter.in.web.dto.ConversationCreateRequest;
import com.mopl.domain.conversation.adapter.in.web.dto.ConversationDto;
import com.mopl.domain.conversation.adapter.in.web.dto.ConversationSearchRequest;
import com.mopl.domain.conversation.adapter.in.web.dto.DirectMessageSearchRequest;
import com.mopl.domain.conversation.adapter.in.web.mapper.ConversationWebMapper;
import com.mopl.domain.conversation.application.dto.ConversationSearchCondition;
import com.mopl.domain.conversation.application.port.in.CreateConversationUseCase;
import com.mopl.domain.conversation.application.port.in.GetConversationListUseCase;
import com.mopl.domain.conversation.application.port.in.GetConversationUseCase;
import com.mopl.domain.conversation.domain.Conversation;
import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.application.port.in.GetDirectMessageListUseCase;
import com.mopl.domain.dm.application.port.in.ReadDirectMessageUseCase;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ConversationControllerTest {

  @InjectMocks
  private ConversationController conversationController;

  @Mock
  private CreateConversationUseCase createConversationUseCase;

  @Mock
  private GetConversationUseCase getConversationUseCase;

  @Mock
  private GetConversationListUseCase getConversationListUseCase;

  @Mock
  private GetDirectMessageListUseCase getDirectMessageListUseCase;

  @Mock
  private ReadDirectMessageUseCase readDirectMessageUseCase;

  @Mock
  private ConversationWebMapper conversationWebMapper;

  private final UUID myId = UUID.randomUUID();
  private final UUID withUserId = UUID.randomUUID();
  private final JwtClaims claims = JwtClaims.builder().userId(myId).build();

  @Test
  @DisplayName("대화 생성 성공")
  void createConversation_success() {
    
    ConversationCreateRequest request = new ConversationCreateRequest(withUserId);
    Conversation conversation = new Conversation(UUID.randomUUID(), myId, withUserId, Instant.now());
    ConversationDto dto = new ConversationDto(
        conversation.getId(),
        new UserSummary(withUserId, "test", null),
        null,
        false
    );

    given(createConversationUseCase.create(myId, withUserId)).willReturn(conversation);
    given(conversationWebMapper.toDto(conversation, myId)).willReturn(dto);

    
    ResponseEntity<ConversationDto> response =
        conversationController.createConversation(request, claims);

    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(conversation.getId());
    then(createConversationUseCase).should().create(myId, withUserId);
  }

  @Test
  @DisplayName("대화 단건 조회 성공")
  void getConversation_success() {
    
    UUID conversationId = UUID.randomUUID();
    Conversation conversation = new Conversation(conversationId, myId, withUserId, Instant.now());
    ConversationDto dto = new ConversationDto(
        conversationId,
        new UserSummary(withUserId, "test", null),
        null,
        false
    );

    given(getConversationUseCase.getById(conversationId, myId)).willReturn(conversation);
    given(conversationWebMapper.toDto(conversation, myId)).willReturn(dto);

    
    ResponseEntity<ConversationDto> response =
        conversationController.getConversation(conversationId, claims);

    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().id()).isEqualTo(conversationId);
  }

  @Test
  @DisplayName("특정 사용자와의 대화 조회 성공")
  void getConversationWith_success() {
    
    Conversation conversation = new Conversation(UUID.randomUUID(), myId, withUserId, Instant.now());
    ConversationDto dto = new ConversationDto(
        conversation.getId(),
        new UserSummary(withUserId, "test", null),
        null,
        false
    );

    given(getConversationUseCase.getByParticipant(myId, withUserId)).willReturn(conversation);
    given(conversationWebMapper.toDto(conversation, myId)).willReturn(dto);

    
    ResponseEntity<ConversationDto> response =
        conversationController.getConversationWith(withUserId, claims);

    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().with().userId()).isEqualTo(withUserId);
  }

  @Test
  @DisplayName("대화 목록 조회 성공")
  void getConversations_success() {
    
    ConversationSearchRequest request = new ConversationSearchRequest(
        null, null, null, 10, "createdAt", "ASCENDING"
    );
    Conversation conversation = new Conversation(UUID.randomUUID(), myId, withUserId, Instant.now());
    ConversationDto dto = new ConversationDto(
        conversation.getId(),
        new UserSummary(withUserId, "test", null),
        null,
        false
    );

    CursorPageResponse<Conversation> domainResponse = new CursorPageResponse<>(
        List.of(conversation), null, null, false, 1, "createdAt", "ASCENDING"
    );

    given(getConversationListUseCase.getList(eq(myId), any(ConversationSearchCondition.class)))
        .willReturn(domainResponse);
    given(conversationWebMapper.toDtoList(List.of(conversation), myId)).willReturn(List.of(dto));


    ResponseEntity<CursorPageResponse<ConversationDto>> response =
        conversationController.getConversations(request, claims);

    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).hasSize(1);
  }

  @Test
  @DisplayName("DM 목록 조회 성공")
  void getDirectMessages_success() {
    
    UUID conversationId = UUID.randomUUID();
    String sortBy = "createdAt";
    SortDirection sortDirection = SortDirection.ASCENDING;
    DirectMessageSearchRequest request = new DirectMessageSearchRequest(
        null, null, 10, sortBy, sortDirection
    );
    Conversation conversation = new Conversation(conversationId, myId, withUserId, Instant.now());

    DirectMessageDto dmDto = new DirectMessageDto(
        UUID.randomUUID(),
        conversationId,
        Instant.now(),
        new UserSummary(myId, "me", null),
        new UserSummary(withUserId, "other", null),
        "안녕"
    );

    CursorPageResponse<DirectMessageDto> dmResponse = new CursorPageResponse<>(
        List.of(dmDto), null, null, false, 1, "createdAt", "ASCENDING"
    );

    given(getConversationUseCase.getById(conversationId, myId)).willReturn(conversation);
    given(getDirectMessageListUseCase.getList(eq(conversationId), any(DirectMessageSearchCondition.class)))
        .willReturn(dmResponse);

    
    ResponseEntity<CursorPageResponse<DirectMessageDto>> response =
        conversationController.getDirectMessages(conversationId, request, claims);

    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).hasSize(1);
    assertThat(response.getBody().data().get(0).content()).isEqualTo("안녕");
  }

  @Test
  @DisplayName("DM 읽음 처리 성공")
  void readDirectMessage_success() {
    
    UUID conversationId = UUID.randomUUID();
    UUID directMessageId = UUID.randomUUID();

    
    ResponseEntity<Void> response =
        conversationController.readDirectMessage(conversationId, directMessageId, claims);

    
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    then(readDirectMessageUseCase).should().read(conversationId, directMessageId, myId);
  }
}