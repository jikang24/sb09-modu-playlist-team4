package com.mopl.domain.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ContentUpdateRequest(
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    String title,

    String description,

    @Size(max = 500, message = "썸네일 URL은 500자 이하여야 합니다.")
    String thumbnailUrl,

    List<String> tags
) {

}
