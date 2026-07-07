package com.mopl.domain.contentchat.dto;

import com.mopl.global.dto.UserSummary;

/** /sub/contents/{contentId}/chat 로 브로드캐스트되는 콘텐츠 채팅 메시지 (DB 저장 없음) */
public record ContentChatDto(
    UserSummary sender,
    String content
) {}