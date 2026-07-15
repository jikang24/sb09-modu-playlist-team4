package com.mopl.domain.watchingsession.application.port.out;

import com.mopl.domain.watchingsession.dto.WatchingSessionChange;
import java.util.UUID;

public interface PublishWatchingSessionEventPort {

  /** 시청자 입장/퇴장을 같은 콘텐츠를 보고 있는 다른 시청자들에게 실시간으로 알림 */
  void publish(UUID contentId, WatchingSessionChange change);
}