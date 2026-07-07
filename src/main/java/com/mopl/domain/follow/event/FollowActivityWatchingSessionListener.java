package com.mopl.domain.follow.event;

import com.mopl.domain.follow.application.port.in.GetFollowerIdsUseCase;
import com.mopl.domain.follow.application.port.out.LoadUserPort;
import com.mopl.domain.notification.domain.NotificationType;
import com.mopl.global.event.NotificationEventPublisher;
import com.mopl.global.event.NotificationRequestedEvent;
import com.mopl.global.event.WatchingSessionStartedEvent;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowActivityWatchingSessionListener {

    private final GetFollowerIdsUseCase getFollowerIdsUseCase;
    private final LoadUserPort loadUserPort;
    private final NotificationEventPublisher notificationEventPublisher;

    // watchingsession은 Redis 기반이라 JPA 트랜잭션이 없어 @TransactionalEventListener를 쓸 수 없음.
    // 팔로워 수만큼 반복 발행하므로 STOMP 처리 스레드를 막지 않도록 @Async로 처리 (FollowActivityPlaylistListener와 동일한 이유).
    @Async("followActivityTaskExecutor")
    @EventListener
    public void onWatchingSessionStarted(WatchingSessionStartedEvent event) {
        List<UUID> followerIds = getFollowerIdsUseCase.getFollowerIds(event.watcherId());
        if (followerIds.isEmpty()) {
            return;
        }

        String watcherName = loadUserPort.getUserSummary(event.watcherId()).name();
        for (UUID followerId : followerIds) {
            notificationEventPublisher.publish(new NotificationRequestedEvent(
                followerId,
                NotificationType.FOLLOW_ACTIVITY.name(),
                "실시간 시청",
                watcherName + "님이 '" + event.contentTitle() + "' 콘텐츠를 시청 중입니다."
            ));
        }
        log.info("팔로우 활동 알림 발행 - watcherId: {}, contentId: {}, followerCount: {}",
            event.watcherId(), event.contentId(), followerIds.size());
    }
}
