package com.mopl.domain.content.service;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.dto.ContentUpdateRequest;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
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

  private ContentSearchRequest makeSearchRequest(int limit) {
    return new ContentSearchRequest(
        null, null, null,
        null, null, limit,
        "createdAt", "DESCENDING"
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
      Content content = makeContent(id, ContentType.TV_SERIES, "tmdb-002");
      given(contentRepository.findById(id)).willReturn(Optional.of(content));

      ContentResponse response = contentService.getContent(id);

      assertThat(response.id()).isEqualTo(id);
      assertThat(response.type()).isEqualTo(ContentType.TV_SERIES);
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
    @DisplayName("다음 페이지 없음 - hasNext=false, nextCursor=null")
    void noNextPage() {
      // given: limit=3인데 데이터 2건만 있음 (limit+1보다 적게 조회됨)
      ContentSearchRequest request = makeSearchRequest(3);

      List<Content> contents = List.of(
          makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-001"),
          makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-002")
      );

      given(contentRepository.findAllByCondition(request)).willReturn(contents);
      given(contentRepository.countByCondition(request)).willReturn(2L);

      // when
      CursorPageResponse<ContentResponse> response = contentService.getContents(request);

      // then
      assertThat(response.data()).hasSize(2);
      assertThat(response.hasNext()).isFalse();
      assertThat(response.nextCursor()).isNull();
      assertThat(response.nextIdAfter()).isNull();
      assertThat(response.totalCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("다음 페이지 있음 - hasNext=true, nextCursor 채워짐")
    void hasNextPage() {
      // given: limit=2인데 데이터 3건 조회됨 (limit+1) → 다음 페이지 있음
      ContentSearchRequest request = makeSearchRequest(2);

      Content first = makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-001");
      Content second = makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-002");
      Content third = makeContent(UUID.randomUUID(), ContentType.MOVIE, "tmdb-003");
      List<Content> contents = List.of(first, second, third); // limit(2)+1=3건

      given(contentRepository.findAllByCondition(request)).willReturn(contents);
      given(contentRepository.countByCondition(request)).willReturn(10L);

      // when
      CursorPageResponse<ContentResponse> response = contentService.getContents(request);

      // then
      assertThat(response.data()).hasSize(2); // limit만큼만 반환
      assertThat(response.hasNext()).isTrue();
      assertThat(response.nextCursor()).isEqualTo(second.getCreatedAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(second.getId());
      assertThat(response.totalCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("빈 결과 - data 비어있고 hasNext=false")
    void emptyResult() {
      // given
      ContentSearchRequest request = makeSearchRequest(20);
      given(contentRepository.findAllByCondition(request)).willReturn(List.of());
      given(contentRepository.countByCondition(request)).willReturn(0L);

      // when
      CursorPageResponse<ContentResponse> response = contentService.getContents(request);

      // then
      assertThat(response.data()).isEmpty();
      assertThat(response.hasNext()).isFalse();
      assertThat(response.totalCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("타입 필터 적용 - typeEqual 조건으로 조회")
    void filterByType() {
      // given
      ContentSearchRequest request = new ContentSearchRequest(
          ContentType.SPORT, null, null,
          null, null, 20,
          "createdAt", "DESCENDING"
      );

      List<Content> sportsContents = List.of(
          makeContent(UUID.randomUUID(), ContentType.SPORT, "sports-001")
      );

      given(contentRepository.findAllByCondition(request)).willReturn(sportsContents);
      given(contentRepository.countByCondition(request)).willReturn(1L);

      // when
      CursorPageResponse<ContentResponse> response = contentService.getContents(request);

      // then
      assertThat(response.data()).hasSize(1);
      assertThat(response.data().get(0).type()).isEqualTo(ContentType.SPORT);
    }

    @Test
    @DisplayName("sortBy, sortDirection이 응답에 그대로 반영된다")
    void sortInfoReflected() {
      // given
      ContentSearchRequest request = new ContentSearchRequest(
          null, null, null,
          null, null, 10,
          "title", "ASCENDING"
      );

      given(contentRepository.findAllByCondition(request)).willReturn(List.of());
      given(contentRepository.countByCondition(request)).willReturn(0L);

      // when
      CursorPageResponse<ContentResponse> response = contentService.getContents(request);

      // then
      assertThat(response.sortBy()).isEqualTo("title");
      assertThat(response.sortDirection()).isEqualTo("ASCENDING");
    }
  }

  @Nested
  @DisplayName("평점 갱신 이벤트 처리 - handleReviewRatingUpdated()")
  class HandleReviewRatingUpdated {

    @Test
    @DisplayName("정상 처리 - 평점/리뷰수 갱신 후 저장")
    void success() {
      UUID id = UUID.randomUUID();
      Content content = makeContent(id, ContentType.MOVIE, "tmdb-001");
      ReviewRatingUpdatedEvent event = new ReviewRatingUpdatedEvent(
          id, new java.math.BigDecimal("4.50"), 15);

      given(contentRepository.findById(id)).willReturn(Optional.of(content));
      given(contentRepository.save(any(Content.class))).willReturn(content);

      contentService.handleReviewRatingUpdated(event);

      then(contentRepository).should().findById(id);
      then(contentRepository).should().save(any(Content.class));
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 - CONTENT_NOT_FOUND 예외")
    void fail_notFound() {
      UUID id = UUID.randomUUID();
      ReviewRatingUpdatedEvent event = new ReviewRatingUpdatedEvent(
          id, new java.math.BigDecimal("3.00"), 5);

      given(contentRepository.findById(id)).willReturn(Optional.empty());

      assertThatThrownBy(() -> contentService.handleReviewRatingUpdated(event))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));

      then(contentRepository).shouldHaveNoMoreInteractions();
    }
  }
}
