package com.mopl.domain.playlist.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.mopl.domain.playlist.application.dto.PlaylistCreateRequest;
import com.mopl.domain.playlist.application.dto.PlaylistDto;
import com.mopl.domain.playlist.application.dto.PlaylistUpdateRequest;
import com.mopl.domain.playlist.application.port.out.LoadContentPort;
import com.mopl.domain.playlist.application.port.out.LoadPlaylistPort;
import com.mopl.domain.playlist.application.port.out.LoadUserPort;
import com.mopl.domain.playlist.application.port.out.SavePlaylistPort;
import com.mopl.domain.playlist.domain.Playlist;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.PlaylistCreatedEvent;
import com.mopl.global.exception.MoplException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock private SavePlaylistPort savePlaylistPort;
    @Mock private LoadPlaylistPort loadPlaylistPort;
    @Mock private LoadUserPort loadUserPort;
    @Mock private LoadContentPort loadContentPort;
    @Mock private NotificationEventPublisher notificationEventPublisher;
    @Mock private ApplicationEventPublisher applicationEventPublisher;

    private PlaylistService playlistService;

    @BeforeEach
    void setUp() {
        playlistService = new PlaylistService(
                savePlaylistPort,
                loadPlaylistPort,
                loadUserPort,
                loadContentPort,
                notificationEventPublisher,
                applicationEventPublisher
        );
    }

    @Test
    @DisplayName("플레이리스트 생성 성공")
    void create_success() {
        // given
        UUID ownerId = UUID.randomUUID();
        PlaylistCreateRequest request = new PlaylistCreateRequest("Title", "Description");
        given(savePlaylistPort.save(any(Playlist.class))).willAnswer(inv -> inv.getArgument(0));
        given(loadUserPort.getUserSummary(ownerId)).willReturn(new UserSummary(ownerId, "Owner", null));

        // when
        PlaylistDto result = playlistService.create(ownerId, request);

        // then
        assertThat(result.title()).isEqualTo("Title");
        verify(savePlaylistPort).save(any(Playlist.class));
        verify(applicationEventPublisher).publishEvent(any(PlaylistCreatedEvent.class));
    }

    @Test
    @DisplayName("플레이리스트 상세 조회 성공")
    void getById_success() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Playlist playlist = Playlist.create(userId, "Title", "Desc");
        given(loadPlaylistPort.findById(playlistId)).willReturn(Optional.of(playlist));
        given(loadUserPort.getUserSummary(userId)).willReturn(new UserSummary(userId, "User", null));

        // when
        PlaylistDto result = playlistService.getById(playlistId, userId);

        // then
        assertThat(result.title()).isEqualTo("Title");
    }

    @Test
    @DisplayName("플레이리스트 수정 성공")
    void update_success() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Playlist playlist = Playlist.create(userId, "Old Title", "Old Desc");
        given(loadPlaylistPort.findById(playlistId)).willReturn(Optional.of(playlist));
        given(savePlaylistPort.save(any(Playlist.class))).willAnswer(inv -> inv.getArgument(0));
        given(loadUserPort.getUserSummary(userId)).willReturn(new UserSummary(userId, "User", null));
        PlaylistUpdateRequest request = new PlaylistUpdateRequest("New Title", "New Desc");

        // when
        PlaylistDto result = playlistService.update(playlistId, userId, request);

        // then
        assertThat(result.title()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("소유자가 아닌 사람이 수정 시 실패")
    void update_fail_notOwner() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        Playlist playlist = Playlist.create(otherId, "Title", "Desc");
        given(loadPlaylistPort.findById(playlistId)).willReturn(Optional.of(playlist));

        // when & then
        assertThatThrownBy(() -> playlistService.update(playlistId, userId, new PlaylistUpdateRequest("T", "D")))
                .isInstanceOf(MoplException.class);
    }

    @Test
    @DisplayName("플레이리스트 삭제 성공")
    void delete_success() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Playlist playlist = Playlist.create(userId, "Title", "Desc");
        given(loadPlaylistPort.findById(playlistId)).willReturn(Optional.of(playlist));

        // when
        playlistService.delete(playlistId, userId);

        // then
        verify(savePlaylistPort).delete(playlistId);
    }

    @Test
    @DisplayName("콘텐츠 추가 성공")
    void addContent_success() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Playlist playlist = Playlist.create(userId, "Title", "Desc");
        given(loadPlaylistPort.findById(playlistId)).willReturn(Optional.of(playlist));
        given(loadContentPort.existsById(contentId)).willReturn(true);

        // when
        playlistService.addContent(playlistId, contentId, userId);

        // then
        assertThat(playlist.getContentIds()).contains(contentId);
    }

    @Test
    @DisplayName("콘텐츠 삭제 성공")
    void removeContent_success() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID contentId = UUID.randomUUID();
        Playlist playlist = Playlist.create(userId, "Title", "Desc");
        playlist.addContent(contentId);
        given(loadPlaylistPort.findById(playlistId)).willReturn(Optional.of(playlist));

        // when
        playlistService.removeContent(playlistId, contentId, userId);

        // then
        assertThat(playlist.getContentIds()).doesNotContain(contentId);
    }

    @Test
    @DisplayName("플레이리스트 구독 성공")
    void subscribe_success() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Playlist playlist = Playlist.create(UUID.randomUUID(), "Title", "Desc");
        given(loadPlaylistPort.findById(playlistId)).willReturn(Optional.of(playlist));
        given(loadPlaylistPort.isSubscribed(playlistId, userId)).willReturn(false);
        given(loadUserPort.getUserSummary(userId)).willReturn(new UserSummary(userId, "User", null));

        // when
        playlistService.subscribe(playlistId, userId);

        // then
        verify(savePlaylistPort).subscribe(playlistId, userId);
        verify(notificationEventPublisher).publish(any());
    }

    @Test
    @DisplayName("플레이리스트 구독 취소 성공")
    void unsubscribe_success() {
        // given
        UUID playlistId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Playlist playlist = Playlist.create(UUID.randomUUID(), "Title", "Desc");
        given(loadPlaylistPort.findById(playlistId)).willReturn(Optional.of(playlist));
        given(loadPlaylistPort.isSubscribed(playlistId, userId)).willReturn(true);

        // when
        playlistService.unsubscribe(playlistId, userId);

        // then
        verify(savePlaylistPort).unsubscribe(playlistId, userId);
    }
}
