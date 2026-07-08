package com.mopl.domain.follow.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mopl.domain.follow.application.port.in.GetFollowerIdsUseCase;
import com.mopl.domain.follow.application.port.out.LoadUserPort;
import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.global.dto.UserSummary;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.WatchingSessionStartedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowActivityWatchingSessionListener 테스트")
class FollowActivityWatchingSessionListenerTest {

    @Mock
    private GetFollowerIdsUseCase getFollowerIdsUseCase;

    @Mock
    private LoadUserPort loadUserPort;

    @Mock
    private NotificationEventPublisher notificationEventPublisher;

    private FollowActivityWatchingSessionListener listener;

    private UUID watcherId;
    private UUID contentId;

    @BeforeEach
    void setUp() {
        listener = new FollowActivityWatchingSessionListener(
            getFollowerIdsUseCase, loadUserPort, notificationEventPublisher);

        watcherId = UUID.randomUUID();
        contentId = UUID.randomUUID();
    }

    @Test
    @DisplayName("성공: 팔로워 각각에게 FOLLOW_ACTIVITY 알림을 발행한다")
    void onWatchingSessionStarted_publishes_notification_to_each_follower() {
        UUID follower1 = UUID.randomUUID();
        UUID follower2 = UUID.randomUUID();
        given(getFollowerIdsUseCase.getFollowerIds(watcherId))
                .willReturn(List.of(follower1, follower2));
        given(loadUserPort.getUserSummary(watcherId))
                .willReturn(new UserSummary(watcherId, "홍길동", null));

        listener.onWatchingSessionStarted(
            new WatchingSessionStartedEvent(watcherId, contentId, "테스트 콘텐츠"));

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(notificationEventPublisher, times(2)).publish(captor.capture());

        List<NotificationRequestedEvent> events = captor.getAllValues();
        assertThatContainsReceiver(events, follower1);
        assertThatContainsReceiver(events, follower2);
        events.forEach(event ->
            org.assertj.core.api.Assertions.assertThat(event.type())
                .isEqualTo(NotificationType.FOLLOW_ACTIVITY.name()));
    }

    @Test
    @DisplayName("성공: 팔로워가 없으면 알림을 발행하지 않는다")
    void onWatchingSessionStarted_no_followers_does_not_publish() {
        given(getFollowerIdsUseCase.getFollowerIds(watcherId)).willReturn(List.of());

        listener.onWatchingSessionStarted(
            new WatchingSessionStartedEvent(watcherId, contentId, "테스트 콘텐츠"));

        verify(notificationEventPublisher, never()).publish(any());
    }

    private void assertThatContainsReceiver(List<NotificationRequestedEvent> events, UUID receiverId) {
        org.assertj.core.api.Assertions.assertThat(events)
                .anyMatch(event -> event.receiverId().equals(receiverId));
    }
}
