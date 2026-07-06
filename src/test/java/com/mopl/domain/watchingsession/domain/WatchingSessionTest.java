package com.mopl.domain.watchingsession.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WatchingSessionTest {

  @Test
  @DisplayName("create() - watcherId/contentId가 설정되고 id/createdAt이 자동 생성된다")
  void create_success() {
    UUID watcherId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    WatchingSession session = WatchingSession.create(watcherId, contentId);

    assertThat(session.id()).isNotNull();
    assertThat(session.watcherId()).isEqualTo(watcherId);
    assertThat(session.contentId()).isEqualTo(contentId);
    assertThat(session.createdAt()).isNotNull();
  }

  @Test
  @DisplayName("create() - 호출할 때마다 id가 다르게 생성된다")
  void create_generatesUniqueId() {
    UUID watcherId = UUID.randomUUID();
    UUID contentId = UUID.randomUUID();

    WatchingSession first = WatchingSession.create(watcherId, contentId);
    WatchingSession second = WatchingSession.create(watcherId, contentId);

    assertThat(first.id()).isNotEqualTo(second.id());
  }
}
