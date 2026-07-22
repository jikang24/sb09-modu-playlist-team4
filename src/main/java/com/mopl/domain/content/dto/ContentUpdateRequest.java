package com.mopl.domain.content.dto;

import com.mopl.domain.content.domain.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ContentUpdateRequest(

    // 프론트에서 유형 수정 UI 자체를 막고 있어 안 보내는 게 정상 케이스 - null이면 기존 유형 유지
    ContentType type,

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    String title,

    String description,

    List<String> tags
) {}
