package com.mopl.global.event;

public interface NotificationEventPublisher {

  void publish(NotificationRequestedEvent event);
}
