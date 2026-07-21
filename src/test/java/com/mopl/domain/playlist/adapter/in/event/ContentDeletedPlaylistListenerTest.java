package com.mopl.domain.playlist.adapter.in.event;

import static org.mockito.BDDMockito.then;

import com.mopl.domain.playlist.application.port.out.SavePlaylistPort;
import com.mopl.global.event.ContentDeletedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentDeletedPlaylistListenerTest {

  @Mock
  private SavePlaylistPort savePlaylistPort;

  @InjectMocks
  private ContentDeletedPlaylistListener listener;

  @Test
  @DisplayName("handle: 이벤트의 contentId를 모든 플레이리스트에서 제거한다")
  void handle_removesContentFromAllPlaylists() {
    UUID contentId = UUID.randomUUID();
    ContentDeletedEvent event = new ContentDeletedEvent(contentId);

    listener.handle(event);

    then(savePlaylistPort).should().removeContentFromAllPlaylists(contentId);
  }
}