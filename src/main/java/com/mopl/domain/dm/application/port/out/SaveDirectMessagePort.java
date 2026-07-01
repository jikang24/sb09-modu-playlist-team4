package com.mopl.domain.dm.application.port.out;

import com.mopl.domain.dm.domain.DirectMessage;

public interface SaveDirectMessagePort {
  DirectMessage save(DirectMessage directMessage);

}
