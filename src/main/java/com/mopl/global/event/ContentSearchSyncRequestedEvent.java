package com.mopl.global.event;

import java.util.UUID;

public record ContentSearchSyncRequestedEvent(
    UUID contentId
) {

}