package com.mopl.domain.playlist.adapter.out.persistence;

import java.util.UUID;

public interface PlaylistSubscriptionCount {
  UUID getPlaylistId();
  Long getCount();
}
