package com.mopl.domain.content.dto;

import com.mopl.domain.content.domain.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ContentUpdateRequest(

    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    ContentType type,

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    String title,

    String description,

    List<String> tags
) {}
