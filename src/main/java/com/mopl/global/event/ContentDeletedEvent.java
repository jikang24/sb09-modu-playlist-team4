package com.mopl.global.event;

import java.util.UUID;

public record ContentDeletedEvent(
    UUID contentId
) {

}
