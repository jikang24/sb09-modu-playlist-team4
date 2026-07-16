package com.mopl.domain.conversation.adapter.in.web.controller;

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
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "다이렉트 메시지")
@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
@Slf4j
public class ConversationController {

  private final CreateConversationUseCase createConversationUseCase;
  private final GetConversationUseCase getConversationUseCase;
  private final GetConversationListUseCase getConversationListUseCase;
  private final GetDirectMessageListUseCase getDirectMessageListUseCase;
  private final ReadDirectMessageUseCase readDirectMessageUseCase;
  private final ConversationWebMapper conversationWebMapper;

  @Operation(summary = "대화 생성")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @PostMapping
  public ResponseEntity<ConversationDto> createConversation(
      @Valid @RequestBody ConversationCreateRequest request,
      @AuthenticationPrincipal JwtClaims claims
  ) {
    UUID myId = claims.getUserId();
    log.info("대화 생성 요청 - myId: {}, withUserId: {}", myId, request.withUserId());
    Conversation conversation = createConversationUseCase.create(myId, request.withUserId());
    log.info("대화 생성 완료 - conversationId: {}", conversation.getId());
    return ResponseEntity.ok(conversationWebMapper.toDto(conversation, myId));
  }

  @Operation(summary = "대화 목록 조회 (커서 페이지네이션)",
      description = "API 요청자 본인의 대화 목록만 조회할 수 있습니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @GetMapping
  public ResponseEntity<CursorPageResponse<ConversationDto>> getConversations(
      @ModelAttribute ConversationSearchRequest request,
      @AuthenticationPrincipal JwtClaims claims
  ) {
    UUID myId = claims.getUserId();
    log.info("대화 목록 조회 요청 - myId: {}, limit: {}, sortBy: {}, sortDirection: {}",
        myId, request.limit(), request.sortBy(), request.sortDirection());

    ConversationSearchCondition condition = new ConversationSearchCondition(
        request.keywordLike(),
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortBy(),
        SortDirection.valueOf(request.sortDirection())
    );

    CursorPageResponse<Conversation> result = getConversationListUseCase.getList(myId, condition);
    log.info("대화 목록 조회 완료 - myId: {}, 조회된 대화 수: {}, hasNext: {}",
        myId, result.data().size(), result.hasNext());

    CursorPageResponse<ConversationDto> response = new CursorPageResponse<>(
        result.data().stream()
            .map(c -> conversationWebMapper.toDto(c, myId))
            .toList(),
        result.nextCursor(),
        result.nextIdAfter(),
        result.hasNext(),
        result.totalCount(),
        result.sortBy(),
        result.sortDirection()
    );

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "대화 조회")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "404", description = "해당 리소스 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @GetMapping("/{conversationId}")
  public ResponseEntity<ConversationDto> getConversation(
      @PathVariable UUID conversationId,
      @AuthenticationPrincipal JwtClaims claims
  ) {
    UUID myId = claims.getUserId();
    log.info("대화 단건 조회 요청 - myId: {}, conversationId: {}", myId, conversationId);
    Conversation conversation = getConversationUseCase.getById(conversationId, myId);
    log.info("대화 단건 조회 완료 - conversationId: {}", conversationId);
    return ResponseEntity.ok(conversationWebMapper.toDto(conversation, myId));
  }

  @Operation(summary = "특정 사용자와의 대화 조회")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "404", description = "해당 리소스 없음"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @GetMapping("/with")
  public ResponseEntity<ConversationDto> getConversationWith(
      @RequestParam UUID userId,
      @AuthenticationPrincipal JwtClaims claims
  ) {
    UUID myId = claims.getUserId();
    log.info("특정 사용자와의 대화 조회 요청 - myId: {}, withUserId: {}", myId, userId);
    Conversation conversation = getConversationUseCase.getByParticipant(myId, userId);
    log.info("특정 사용자와의 대화 조회 완료 - conversationId: {}", conversation.getId());
    return ResponseEntity.ok(conversationWebMapper.toDto(conversation, myId));
  }

  @Operation(summary = "DM 목록 조회 (커서 페이지네이션)",
      description = "특정 대화의 DM 목록을 조회합니다. API 요청자가 해당 대화의 참여자여야 합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @GetMapping("/{conversationId}/direct-messages")
  public ResponseEntity<CursorPageResponse<DirectMessageDto>> getDirectMessages(
      @PathVariable UUID conversationId,
      @ModelAttribute DirectMessageSearchRequest request,
      @AuthenticationPrincipal JwtClaims claims
  ) {
    UUID myId = claims.getUserId();
    log.info("DM 목록 조회 요청 - myId: {}, conversationId: {}, limit: {}",
        myId, conversationId, request.limit());

    getConversationUseCase.getById(conversationId, myId);

    DirectMessageSearchCondition condition = new DirectMessageSearchCondition(
        request.cursor(),
        request.idAfter(),
        request.limit(),
        request.sortDirection(),
        request.sortBy()
    );

    CursorPageResponse<DirectMessageDto> response = getDirectMessageListUseCase
        .getList(conversationId, condition);

    log.info("DM 목록 조회 완료 - conversationId: {}, 조회된 DM 수: {}, hasNext: {}",
        conversationId, response.data().size(), response.hasNext());

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "DM 읽음 처리")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청"),
      @ApiResponse(responseCode = "401", description = "인증 오류"),
      @ApiResponse(responseCode = "500", description = "서버 오류")
  })
  @PostMapping("/{conversationId}/direct-messages/{directMessageId}/read")
  public ResponseEntity<Void> readDirectMessage(
      @PathVariable UUID conversationId,
      @PathVariable UUID directMessageId,
      @AuthenticationPrincipal JwtClaims claims
  ) {
    UUID myId = claims.getUserId();
    log.info("DM 읽음 처리 요청 - myId: {}, conversationId: {}, directMessageId: {}",
        myId, conversationId, directMessageId);
    readDirectMessageUseCase.read(conversationId, directMessageId, myId);
    log.info("DM 읽음 처리 완료 - directMessageId: {}", directMessageId);
    return ResponseEntity.ok().build();
  }
}