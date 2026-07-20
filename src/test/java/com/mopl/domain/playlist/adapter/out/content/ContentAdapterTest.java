package com.mopl.domain.playlist.adapter.out.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.config.PlaceholderImageController;
import com.mopl.global.dto.ContentSummary;
import com.mopl.infra.s3.S3Service;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentAdapterTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private S3Service s3Service;

  @InjectMocks
  private ContentAdapter adapter;

  @Test
  @DisplayName("findSummariesByIds: S3 key(스킴 없음)로 저장된 썸네일은 presigned URL로 치환한다")
  void findSummariesByIds_resolvesS3KeyToPresignedUrl() {
    Content content = Content.create(
        ContentType.MOVIE, Content.MANUAL_EXTERNAL_ID_PREFIX + "1",
        "제목", "설명", "thumbnail-key.jpg", List.of());

    given(contentRepository.findAllByIds(List.of(content.getId()))).willReturn(List.of(content));
    given(s3Service.getPresignedUrl("thumbnail-key.jpg"))
        .willReturn("https://signed.example.com/thumbnail-key.jpg?sig=abc");

    List<ContentSummary> result = adapter.findSummariesByIds(List.of(content.getId()));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).thumbnailUrl())
        .isEqualTo("https://signed.example.com/thumbnail-key.jpg?sig=abc");
  }

  @Test
  @DisplayName("findSummariesByIds: 스킴이 있는 외부 URL(TMDB 등)은 그대로 반환한다")
  void findSummariesByIds_keepsExternalUrlAsIs() {
    Content content = Content.create(
        ContentType.MOVIE, "tmdb-1", "제목", "설명", "https://image.tmdb.org/poster.jpg", List.of());

    given(contentRepository.findAllByIds(List.of(content.getId()))).willReturn(List.of(content));

    List<ContentSummary> result = adapter.findSummariesByIds(List.of(content.getId()));

    assertThat(result.get(0).thumbnailUrl()).isEqualTo("https://image.tmdb.org/poster.jpg");
  }

  @Test
  @DisplayName("findSummariesByIds: 썸네일이 없으면 placeholder 경로를 반환한다")
  void findSummariesByIds_returnsPlaceholderWhenBlank() {
    Content content = Content.create(
        ContentType.MOVIE, "tmdb-2", "제목", "설명", null, List.of());

    given(contentRepository.findAllByIds(List.of(content.getId()))).willReturn(List.of(content));

    List<ContentSummary> result = adapter.findSummariesByIds(List.of(content.getId()));

    assertThat(result.get(0).thumbnailUrl()).isEqualTo(PlaceholderImageController.PATH);
  }
}
