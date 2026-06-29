package com.mopl.domain.content.service;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentUpdateRequest;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

  @Mock
  private ContentRepository contentRepository;

  @InjectMocks
  private ContentService contentService;

  private Content makeContent(UUID id, ContentType type, String externalId) {
    return Content.restore(
        id, type, externalId,
        "테스트 제목", "테스트 설명", "https://thumbnail.jpg",
        BigDecimal.ZERO, 0,
        java.time.Instant.now(), java.time.Instant.now(),
        List.of("액션", "SF")
    );
  }

  private ContentCreateRequest makeCreateRequest(ContentType type, String externalId) {
    return new ContentCreateRequest(
        type, externalId,
        "테스트 제목", "테스트 설명",
        List.of("액션", "SF")
    );
  }

  @Nested
  @DisplayName("콘텐츠 등록")
  class CreateContent {

    @Test
    @DisplayName("정상 등록 - 새로운 콘텐츠가 저장")
    void success() {
      ContentCreateRequest request = makeCreateRequest(ContentType.MOVIE, "tmdb-001");
      MultipartFile thumbnail = mock(MultipartFile.class);

      UUID savedId = UUID.randomUUID();
      Content saved = makeContent(savedId, ContentType.MOVIE, "tmdb-001");

      given(contentRepository.findByTypeAndExternalId(ContentType.MOVIE, "tmdb-001"))
          .willReturn(Optional.empty());
      given(contentRepository.save(any(Content.class)))
          .willReturn(saved);

      ContentResponse response = contentService.createContent(request, thumbnail);

      assertThat(response.id()).isEqualTo(savedId);
      assertThat(response.type()).isEqualTo(ContentType.MOVIE);
      assertThat(response.title()).isEqualTo("테스트 제목");
      then(contentRepository).should().save(any(Content.class));
    }

    @Test
    @DisplayName("중복 등록 - 같은 타입+외부ID면 예외 발생")
    void fail_duplicate() {
      ContentCreateRequest request = makeCreateRequest(ContentType.MOVIE, "tmdb-001");
      MultipartFile thumbnail = mock(MultipartFile.class);

      Content existing = makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-001");

      given(contentRepository.findByTypeAndExternalId(ContentType.MOVIE, "tmdb-001"))
          .willReturn(Optional.of(existing));

      assertThatThrownBy(() -> contentService.createContent(request, thumbnail))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.CONTENT_ALREADY_EXISTS));


      then(contentRepository).should().findByTypeAndExternalId(any(), any());
      then(contentRepository).shouldHaveNoMoreInteractions();
    }
  }

  @Nested
  @DisplayName("콘텐츠 수정")
  class UpdateContent {

    @Test
    @DisplayName("정상 수정 - 제목/설명/태그 변경")
    void success() {

      UUID id = UUID.randomUUID();
      Content existing = makeContent(id, ContentType.MOVIE, "tmdb-001");

      ContentUpdateRequest request = new ContentUpdateRequest(
          "수정된 제목", "수정된 설명", List.of("드라마")
      );
      MultipartFile thumbnail = mock(MultipartFile.class);

      given(contentRepository.findById(id)).willReturn(Optional.of(existing));
      given(contentRepository.save(any(Content.class))).willReturn(existing);

      ContentResponse response = contentService.updateContent(id, request, thumbnail);

      assertThat(response).isNotNull();
      then(contentRepository).should().save(any(Content.class));
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 수정 - 예외 발생")
    void fail_notFound() {
      UUID id = UUID.randomUUID();
      given(contentRepository.findById(id)).willReturn(Optional.empty());

      ContentUpdateRequest request = new ContentUpdateRequest(
          "수정된 제목", "수정된 설명", List.of()
      );

      assertThatThrownBy(() ->
          contentService.updateContent(id, request, mock(MultipartFile.class)))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
    }
  }


  @Nested
  @DisplayName("콘텐츠 삭제")
  class DeleteContent {

    @Test
    @DisplayName("정상 삭제 - deleteById 호출")
    void success() {

      UUID id = UUID.randomUUID();
      given(contentRepository.existsById(id)).willReturn(true);

      contentService.deleteContent(id);

      then(contentRepository).should().deleteById(id);
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 삭제 - 예외 발생")
    void fail_notFound() {
      UUID id = UUID.randomUUID();
      given(contentRepository.existsById(id)).willReturn(false);

      assertThatThrownBy(() -> contentService.deleteContent(id))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));

      then(contentRepository).should().existsById(id);
      then(contentRepository).shouldHaveNoMoreInteractions();
    }
  }

  @Nested
  @DisplayName("콘텐츠 단건 조회")
  class GetContent {

    @Test
    @DisplayName("정상 조회 - ContentResponse 반환")
    void success() {
      UUID id = UUID.randomUUID();
      Content content = makeContent(id, ContentType.DRAMA, "tmdb-002");
      given(contentRepository.findById(id)).willReturn(Optional.of(content));

      ContentResponse response = contentService.getContent(id);

      assertThat(response.id()).isEqualTo(id);
      assertThat(response.type()).isEqualTo(ContentType.DRAMA);
    }

    @Test
    @DisplayName("존재하지 않는 ID 조회 - 예외 발생")
    void fail_notFound() {
      UUID id = UUID.randomUUID();
      given(contentRepository.findById(id)).willReturn(Optional.empty());

      assertThatThrownBy(() -> contentService.getContent(id))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
    }
  }

  @Nested
  @DisplayName("콘텐츠 목록 조회")
  class GetContents {

    @Test
    @DisplayName("전체 목록 조회 - 저장된 콘텐츠 수만큼 반환")
    void getAll() {
      List<Content> contents = List.of(
          makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-001"),
          makeContent(UUID.randomUUID(), ContentType.DRAMA, "tmdb-002"),
          makeContent(UUID.randomUUID(), ContentType.SPORTS, "sports-001")
      );
      given(contentRepository.findAll()).willReturn(contents);

      List<ContentResponse> responses = contentService.getContents();

      assertThat(responses).hasSize(3);
    }

    @Test
    @DisplayName("타입별 조회 - MOVIE 타입만 반환")
    void getByType() {
      List<Content> movies = List.of(
          makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-001"),
          makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-002")
      );
      given(contentRepository.findAllByType(ContentType.MOVIE)).willReturn(movies);

      List<ContentResponse> responses = contentService.getContentsByType(ContentType.MOVIE);

      assertThat(responses).hasSize(2);
      assertThat(responses).allSatisfy(r ->
          assertThat(r.type()).isEqualTo(ContentType.MOVIE));
    }

    @Test
    @DisplayName("필수값 누락 시 예외 발생")
    void fail_nullTitle() {
      assertThatThrownBy(() ->
          Content.create(ContentType.MOVIE, "tmdb-001", null, "설명", null, List.of()))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.INVALID_INPUT));
    }
  }
}