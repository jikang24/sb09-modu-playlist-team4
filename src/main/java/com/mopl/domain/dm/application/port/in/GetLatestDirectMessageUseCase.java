package com.mopl.domain.dm.application.port.in;

import com.mopl.domain.dm.domain.DirectMessage;
import java.util.Optional;
import java.util.UUID;
import javax.swing.text.html.Option;

public interface GetLatestDirectMessageUseCase {
  Optional<DirectMessage> getLatest(UUID conversationId);
}
