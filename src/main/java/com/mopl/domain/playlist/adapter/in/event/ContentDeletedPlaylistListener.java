package com.mopl.domain.playlist.adapter.in.event;

import com.mopl.domain.playlist.application.port.out.SavePlaylistPort;
import com.mopl.global.event.ContentDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 콘텐츠가 삭제됐을 때, 그 콘텐츠를 담고 있던 모든 플레이리스트에서 함께 제거
 * @Async + AFTER_COMMIT: 콘텐츠 삭제 트랜잭션이 커밋된 후, 별도 스레드에서 처리
 */
@Component
@RequiredArgsConstructor
public class ContentDeletedPlaylistListener {

  private final SavePlaylistPort savePlaylistPort;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(ContentDeletedEvent event) {
    savePlaylistPort.removeContentFromAllPlaylists(event.contentId());
  }
}