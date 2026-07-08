package com.mopl.domain.follow.event;

import com.mopl.domain.follow.application.port.in.GetFollowerIdsUseCase;
import com.mopl.domain.follow.application.port.out.LoadUserPort;
import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.PlaylistCreatedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowActivityPlaylistListener {

    private final GetFollowerIdsUseCase getFollowerIdsUseCase;
    private final LoadUserPort loadUserPort;
    private final NotificationEventPublisher notificationEventPublisher;

    // 팔로워 수만큼 Kafka publish를 반복하므로 요청 스레드를 막지 않도록 @Async로 별도 스레드에서 처리.
    @Async("followActivityTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPlaylistCreated(PlaylistCreatedEvent event) {
        List<UUID> followerIds = getFollowerIdsUseCase.getFollowerIds(event.ownerId());
        if (followerIds.isEmpty()) {
            return;
        }

        String ownerName = loadUserPort.getUserSummary(event.ownerId()).name();
        for (UUID followerId : followerIds) {
            notificationEventPublisher.publish(new NotificationRequestedEvent(
                followerId,
                NotificationType.FOLLOW_ACTIVITY.name(),
                "팔로우 활동",
                ownerName + "님이 '" + event.title() + "' 플레이리스트를 등록했습니다."
            ));
        }
        log.info("팔로우 활동 알림 발행 - playlistId: {}, ownerId: {}, followerCount: {}",
            event.playlistId(), event.ownerId(), followerIds.size());
    }
}
