package com.mopl.domain.playlist.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mopl.global.exception.MoplException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlaylistTest {

    @Test
    @DisplayName("플레이리스트 생성 성공")
    void create_success() {
        // given
        UUID ownerId = UUID.randomUUID();
        String title = "Test Playlist";
        String description = "Test Description";

        // when
        Playlist playlist = Playlist.create(ownerId, title, description);

        // then
        assertThat(playlist.getId()).isNotNull();
        assertThat(playlist.getOwnerId()).isEqualTo(ownerId);
        assertThat(playlist.getTitle()).isEqualTo(title);
        assertThat(playlist.getDescription()).isEqualTo(description);
        assertThat(playlist.getCreatedAt()).isNotNull();
        assertThat(playlist.getUpdatedAt()).isNotNull();
        assertThat(playlist.getContentIds()).isEmpty();
    }

    @Test
    @DisplayName("제목이 비어있으면 플레이리스트 생성 실패")
    void create_fail_emptyTitle() {
        UUID ownerId = UUID.randomUUID();
        assertThatThrownBy(() -> Playlist.create(ownerId, "", "description"))
                .isInstanceOf(MoplException.class);
        assertThatThrownBy(() -> Playlist.create(ownerId, null, "description"))
                .isInstanceOf(MoplException.class);
    }

    @Test
    @DisplayName("플레이리스트 복원 성공")
    void restore_success() {
        // given
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        String title = "Restored Playlist";
        String description = "Restored Description";
        Instant now = Instant.now();
        List<UUID> contentIds = List.of(UUID.randomUUID());

        // when
        Playlist playlist = Playlist.restore(id, ownerId, title, description, now, now, contentIds);

        // then
        assertThat(playlist.getId()).isEqualTo(id);
        assertThat(playlist.getOwnerId()).isEqualTo(ownerId);
        assertThat(playlist.getTitle()).isEqualTo(title);
        assertThat(playlist.getContentIds()).hasSize(1);
    }

    @Test
    @DisplayName("플레이리스트 정보 수정 성공")
    void update_success() {
        // given
        Playlist playlist = Playlist.create(UUID.randomUUID(), "Old Title", "Old Desc");
        Instant oldUpdatedAt = playlist.getUpdatedAt();

        // when
        playlist.update("New Title", "New Desc");

        // then
        assertThat(playlist.getTitle()).isEqualTo("New Title");
        assertThat(playlist.getDescription()).isEqualTo("New Desc");
        assertThat(playlist.getUpdatedAt()).isAfterOrEqualTo(oldUpdatedAt);
    }

    @Test
    @DisplayName("플레이리스트 콘텐츠 추가 성공")
    void addContent_success() {
        // given
        Playlist playlist = Playlist.create(UUID.randomUUID(), "Title", "Desc");
        UUID contentId = UUID.randomUUID();

        // when
        playlist.addContent(contentId);

        // then
        assertThat(playlist.getContentIds()).contains(contentId);
    }

    @Test
    @DisplayName("이미 존재하는 콘텐츠 추가 시 실패")
    void addContent_fail_alreadyExists() {
        // given
        Playlist playlist = Playlist.create(UUID.randomUUID(), "Title", "Desc");
        UUID contentId = UUID.randomUUID();
        playlist.addContent(contentId);

        // when & then
        assertThatThrownBy(() -> playlist.addContent(contentId))
                .isInstanceOf(MoplException.class);
    }

    @Test
    @DisplayName("플레이리스트 콘텐츠 삭제 성공")
    void removeContent_success() {
        // given
        Playlist playlist = Playlist.create(UUID.randomUUID(), "Title", "Desc");
        UUID contentId = UUID.randomUUID();
        playlist.addContent(contentId);

        // when
        playlist.removeContent(contentId);

        // then
        assertThat(playlist.getContentIds()).doesNotContain(contentId);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 삭제 시 실패")
    void removeContent_fail_notFound() {
        // given
        Playlist playlist = Playlist.create(UUID.randomUUID(), "Title", "Desc");
        UUID contentId = UUID.randomUUID();

        // when & then
        assertThatThrownBy(() -> playlist.removeContent(contentId))
                .isInstanceOf(MoplException.class);
    }

    @Test
    @DisplayName("소유자 확인 성공")
    void isOwner_success() {
        // given
        UUID ownerId = UUID.randomUUID();
        Playlist playlist = Playlist.create(ownerId, "Title", "Desc");

        // when & then
        assertThat(playlist.isOwner(ownerId)).isTrue();
        assertThat(playlist.isOwner(UUID.randomUUID())).isFalse();
    }
}
