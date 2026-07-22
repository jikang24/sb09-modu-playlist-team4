package com.mopl.domain.dm.application.port.out;

import com.mopl.domain.dm.domain.DirectMessage;
import java.time.Instant;
import java.util.UUID;

public interface SaveDirectMessagePort {
  DirectMessage save(DirectMessage directMessage);

  /**
   * 프론트는 방을 열 때 "가장 최근 메시지" 하나만 읽음 처리 API를 호출한다.
   * 그 사이 쌓인 이전 안 읽은 메시지들까지 한 번에 읽음 처리하기 위해,
   * 해당 대화방에서 내가 수신자인 메시지 중 주어진 시각 이전(포함)의 안 읽은 메시지를
   * 전부 읽음 처리한다.
   */
  void markAllAsReadUpTo(UUID conversationId, UUID receiverId, Instant upToCreatedAt);

}
