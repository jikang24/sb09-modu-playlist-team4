package com.mopl.domain.dm.adapter.in.web.mapper;

import com.mopl.domain.dm.application.port.out.LoadUserPort;
import com.mopl.domain.dm.domain.DirectMessage;
import com.mopl.global.dto.DirectMessageDto;
import com.mopl.global.dto.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DirectMessageWebMapper {
  private final LoadUserPort loadUserPort;
  public DirectMessageDto toDto(DirectMessage dm){
    UserSummary sender = loadUserPort.getUserSummary(dm.getSenderId());
    UserSummary receiver = loadUserPort.getUserSummary(dm.getReceiverId());
    return new DirectMessageDto(dm.getId(),dm.getConversationId(),dm.getCreatedAt(),sender,receiver,dm.getContent());
  }

}
