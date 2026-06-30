package com.mopl.domain.notification.dto;

import com.mopl.domain.notification.domain.NotificationType;

public enum NotificationLevel {
  INFO,
  WARNING,
  ERROR;

  public static NotificationLevel fromType(NotificationType type) {
    return switch (type) {
      case ROLE_CHANGED -> WARNING;
      case PLAYLIST_SUBSCRIBED, PLAYLIST_UPDATED, FOLLOW, FOLLOW_ACTIVITY, DIRECT_MESSAGE -> INFO;
    };
  }
}
