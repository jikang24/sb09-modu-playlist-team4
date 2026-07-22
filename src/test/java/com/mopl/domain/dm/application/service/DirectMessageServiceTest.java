package com.mopl.domain.dm.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mopl.domain.dm.application.dto.DirectMessageSearchCondition;
import com.mopl.domain.dm.application.port.out.LoadDirectMessagePort;
import com.mopl.domain.dm.application.port.out.LoadUserPort;
import com.mopl.domain.dm.application.port.out.SaveDirectMessagePort;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.SortDirection;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DirectMessageServiceTest {

  @InjectMocks
  private DirectMessageService directMessageService;

  @Mock
  private LoadDirectMessagePort loadDirectMessagePort;

  @Mock
  private SaveDirectMessagePort saveDirectMessagePort;

  @Mock
  private LoadUserPort loadUserPort;

  private final UUID conversationId = UUID.randomUUID();
  private final UUID senderId = UUID.randomUUID();
  private final UUID receiverId = UUID.randomUUID();

  @Test
  @DisplayName("DM 전송 성공")
  void send_success() {
    
    String content = "안녕하세요";
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, content);
    given(saveDirectMessagePort.save(any(DirectMessage.class))).willReturn(directMessage);

    
    DirectMessage result = directMessageService.send(conversationId, content, senderId, receiverId);

    
    assertThat(result).isNotNull();
    assertThat(result.getContent()).isEqualTo(content);
    assertThat(result.getSenderId()).isEqualTo(senderId);
    assertThat(result.getReceiverId()).isEqualTo(receiverId);
    then(saveDirectMessagePort).should().save(any(DirectMessage.class));
  }

  @Test
  @DisplayName("DM 전송 실패 - 내용이 빈 문자열")
  void send_fail_blank_content() {
    
    assertThatThrownBy(() ->
        directMessageService.send(conversationId, "", senderId, receiverId))
        .isInstanceOf(MoplException.class);
    then(saveDirectMessagePort).should(never()).save(any());
  }

  @Test
  @DisplayName("DM 전송 실패 - 내용이 null")
  void send_fail_null_content() {
    
    assertThatThrownBy(() ->
        directMessageService.send(conversationId, null, senderId, receiverId))
        .isInstanceOf(MoplException.class);
    then(saveDirectMessagePort).should(never()).save(any());
  }

  @Test
  @DisplayName("DM 읽음 처리 성공")
  void read_success() {
    
    UUID directMessageId = UUID.randomUUID();
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, "안녕");
    given(loadDirectMessagePort.findById(directMessageId)).willReturn(Optional.of(directMessage));
    given(saveDirectMessagePort.save(any(DirectMessage.class))).willReturn(directMessage);

    
    directMessageService.read(conversationId, directMessageId, receiverId);

    
    assertThat(directMessage.isRead()).isTrue();
    then(saveDirectMessagePort).should().save(directMessage);
  }

  @Test
  @DisplayName("DM 읽음 처리 실패 - 존재하지 않는 DM")
  void read_fail_not_found() {
    
    UUID directMessageId = UUID.randomUUID();
    given(loadDirectMessagePort.findById(directMessageId)).willReturn(Optional.empty());

    
    assertThatThrownBy(() ->
        directMessageService.read(conversationId, directMessageId, receiverId))
        .isInstanceOf(MoplException.class);
    then(saveDirectMessagePort).should(never()).save(any());
  }

  @Test
  @DisplayName("DM 읽음 처리 - 발신자 본인이 호출하면 예외 없이 조용히 무시(no-op)")
  void read_sender_self_call_is_noop() {
    // 프론트가 웹소켓으로 들어오는 모든 메시지(본인이 보낸 것 포함)에 대해 읽음 처리 API를
    // 호출하기 때문에, 발신자 본인의 호출은 에러가 아니라 아무 동작 없이 넘어가야 한다.

    UUID directMessageId = UUID.randomUUID();
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, "안녕");
    given(loadDirectMessagePort.findById(directMessageId)).willReturn(Optional.of(directMessage));


    directMessageService.read(conversationId, directMessageId, senderId);


    assertThat(directMessage.isRead()).isFalse();
    then(saveDirectMessagePort).should(never()).save(any());
  }

  @Test
  @DisplayName("DM 읽음 처리 실패 - 대화 참여자가 아닌 제3자가 읽음 처리 시도 (IDOR 방지)")
  void read_fail_third_party_cannot_read() {

    UUID directMessageId = UUID.randomUUID();
    UUID strangerId = UUID.randomUUID();
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, "안녕");
    given(loadDirectMessagePort.findById(directMessageId)).willReturn(Optional.of(directMessage));


    assertThatThrownBy(() ->
        directMessageService.read(conversationId, directMessageId, strangerId))
        .isInstanceOf(MoplException.class);
    then(saveDirectMessagePort).should(never()).save(any());
  }

  @Test
  @DisplayName("DM 읽음 처리 실패 - 경로의 conversationId와 실제 메시지가 속한 대화가 다름")
  void read_fail_conversationId_mismatch() {

    UUID directMessageId = UUID.randomUUID();
    UUID otherConversationId = UUID.randomUUID();
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, "안녕");
    given(loadDirectMessagePort.findById(directMessageId)).willReturn(Optional.of(directMessage));


    assertThatThrownBy(() ->
        directMessageService.read(otherConversationId, directMessageId, receiverId))
        .isInstanceOf(MoplException.class);
    then(saveDirectMessagePort).should(never()).save(any());
  }

  @Test
  @DisplayName("최근 DM 조회 성공")
  void getLatest_success() {
    
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, "안녕");
    given(loadDirectMessagePort.findLatestByConversationId(conversationId))
        .willReturn(Optional.of(directMessage));

    
    Optional<DirectMessage> result = directMessageService.getLatest(conversationId);

    
    assertThat(result).isPresent();
    assertThat(result.get().getContent()).isEqualTo("안녕");
  }

  @Test
  @DisplayName("최근 DM 조회 - DM 없는 경우")
  void getLatest_empty() {
    
    given(loadDirectMessagePort.findLatestByConversationId(conversationId))
        .willReturn(Optional.empty());

    
    Optional<DirectMessage> result = directMessageService.getLatest(conversationId);

    
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("읽지 않은 DM 있는지 확인 - 있는 경우")
  void hasUnread_true() {
    
    given(loadDirectMessagePort.hasUnRead(conversationId, receiverId)).willReturn(true);

    
    boolean result = directMessageService.hasUnread(conversationId, receiverId);

    
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("읽지 않은 DM 있는지 확인 - 없는 경우")
  void hasUnread_false() {
    
    given(loadDirectMessagePort.hasUnRead(conversationId, receiverId)).willReturn(false);

    
    boolean result = directMessageService.hasUnread(conversationId, receiverId);

    
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("DM 목록 조회 성공")
  void getList_success() {

    DirectMessageSearchCondition condition = new DirectMessageSearchCondition(
        null, null, 10,  SortDirection.ASCENDING,"createdAt"
    );
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, "안녕");
    UserSummary senderSummary = new UserSummary(senderId, "sender", null);
    UserSummary receiverSummary = new UserSummary(receiverId, "receiver", null);

    CursorPageResponse<DirectMessage> dmResponse = new CursorPageResponse<>(
        List.of(directMessage), null, null, false, 1, "createdAt", "ASCENDING"
    );
    given(loadDirectMessagePort.findList(conversationId, condition)).willReturn(dmResponse);
    given(loadUserPort.getUserSummaries(anyCollection()))
        .willReturn(Map.of(senderId, senderSummary, receiverId, receiverSummary));


    CursorPageResponse<DirectMessageDto> result = directMessageService.getList(conversationId, condition);


    assertThat(result).isNotNull();
    assertThat(result.data()).hasSize(1);
    assertThat(result.data().get(0).content()).isEqualTo("안녕");
    assertThat(result.data().get(0).sender().userId()).isEqualTo(senderId);
    assertThat(result.data().get(0).receiver().userId()).isEqualTo(receiverId);
  }

  @Test
  @DisplayName("DM 목록 조회 시 유저 정보는 건별이 아니라 한 번의 벌크 호출로만 조회한다 (N+1 회귀 방지)")
  void getList_usesBulkUserLookup_notPerItem() {

    DirectMessageSearchCondition condition = new DirectMessageSearchCondition(
        null, null, 10, SortDirection.ASCENDING, "createdAt"
    );
    DirectMessage dm1 = DirectMessage.create(conversationId, senderId, receiverId, "메시지1");
    DirectMessage dm2 = DirectMessage.create(conversationId, receiverId, senderId, "메시지2");
    UserSummary senderSummary = new UserSummary(senderId, "sender", null);
    UserSummary receiverSummary = new UserSummary(receiverId, "receiver", null);

    CursorPageResponse<DirectMessage> dmResponse = new CursorPageResponse<>(
        List.of(dm1, dm2), null, null, false, 2, "createdAt", "ASCENDING"
    );
    given(loadDirectMessagePort.findList(conversationId, condition)).willReturn(dmResponse);
    given(loadUserPort.getUserSummaries(anyCollection()))
        .willReturn(Map.of(senderId, senderSummary, receiverId, receiverSummary));


    directMessageService.getList(conversationId, condition);


    verify(loadUserPort, times(1)).getUserSummaries(anyCollection());
    then(loadUserPort).should(never()).getUserSummary(any());
  }

  @Test
  @DisplayName("DM이 없는 목록 조회 시 유저 조회를 아예 호출하지 않는다")
  void getList_emptyList_skipsUserLookup() {

    DirectMessageSearchCondition condition = new DirectMessageSearchCondition(
        null, null, 10, SortDirection.ASCENDING, "createdAt"
    );
    CursorPageResponse<DirectMessage> dmResponse = new CursorPageResponse<>(
        List.of(), null, null, false, 0, "createdAt", "ASCENDING"
    );
    given(loadDirectMessagePort.findList(conversationId, condition)).willReturn(dmResponse);


    CursorPageResponse<DirectMessageDto> result = directMessageService.getList(conversationId, condition);


    assertThat(result.data()).isEmpty();
    then(loadUserPort).should(never()).getUserSummaries(any());
  }

  @Test
  @DisplayName("여러 대화방의 최근 메시지를 한 번에 조회한다")
  void getLatestBulk_success() {

    UUID otherConversationId = UUID.randomUUID();
    DirectMessage directMessage = DirectMessage.create(conversationId, senderId, receiverId, "안녕");
    given(loadDirectMessagePort.findLatestByConversationIds(List.of(conversationId, otherConversationId)))
        .willReturn(Map.of(conversationId, directMessage));


    Map<UUID, DirectMessage> result =
        directMessageService.getLatestBulk(List.of(conversationId, otherConversationId));


    assertThat(result).hasSize(1);
    assertThat(result.get(conversationId).getContent()).isEqualTo("안녕");
  }

  @Test
  @DisplayName("대화방 ID 목록이 비어있으면 포트를 호출하지 않고 빈 Map을 반환한다")
  void getLatestBulk_emptyInput_skipsPortCall() {

    Map<UUID, DirectMessage> result = directMessageService.getLatestBulk(List.of());


    assertThat(result).isEmpty();
    then(loadDirectMessagePort).should(never()).findLatestByConversationIds(any());
  }

  @Test
  @DisplayName("여러 대화방 중 안 읽은 메시지가 있는 대화방만 한 번에 조회한다")
  void hasUnreadBulk_success() {

    UUID otherConversationId = UUID.randomUUID();
    given(loadDirectMessagePort.findConversationIdsWithUnread(
        List.of(conversationId, otherConversationId), receiverId))
        .willReturn(Set.of(conversationId));


    Set<UUID> result =
        directMessageService.hasUnreadBulk(List.of(conversationId, otherConversationId), receiverId);


    assertThat(result).containsExactly(conversationId);
  }

  @Test
  @DisplayName("대화방 ID 목록이 비어있으면 포트를 호출하지 않고 빈 Set을 반환한다")
  void hasUnreadBulk_emptyInput_skipsPortCall() {

    Set<UUID> result = directMessageService.hasUnreadBulk(List.of(), receiverId);


    assertThat(result).isEmpty();
    then(loadDirectMessagePort).should(never()).findConversationIdsWithUnread(any(), any());
  }

  @Test
  @DisplayName("키워드로 대화방 ID 목록 조회 성공")
  void findConversationIdsByContent_success() {
    
    String keyword = "안녕";
    List<UUID> conversationIds = List.of(conversationId, UUID.randomUUID());
    given(loadDirectMessagePort.findConversationIdsByContent(keyword)).willReturn(conversationIds);

    
    List<UUID> result = directMessageService.findConversationIdsByContent(keyword);

    
    assertThat(result).hasSize(2);
    assertThat(result).contains(conversationId);
  }

  @Test
  @DisplayName("키워드로 대화방 ID 목록 조회 - 결과 없음")
  void findConversationIdsByContent_empty() {
    
    String keyword = "없는내용";
    given(loadDirectMessagePort.findConversationIdsByContent(keyword)).willReturn(List.of());

    
    List<UUID> result = directMessageService.findConversationIdsByContent(keyword);

    
    assertThat(result).isEmpty();
  }
}