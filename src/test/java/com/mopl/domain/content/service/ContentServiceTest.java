package com.mopl.domain.content.service;

import com.mopl.domain.content.adapter.port.LoadWatcherCountPort;
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
import com.mopl.infra.s3.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private LoadWatcherCountPort loadWatcherCountPort;

  @Mock
  private S3Service s3Service;

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

  private ContentCreateRequest makeCreateRequest(ContentType type) {
    return new ContentCreateRequest(
        type,
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
    @DisplayName("정상 등록 - 새로운 콘텐츠가 저장되고, 외부 ID는 서버에서 생성된다")
    void success() {
      ContentCreateRequest request = makeCreateRequest(ContentType.MOVIE);
      MultipartFile thumbnail = mock(MultipartFile.class);

      UUID savedId = UUID.randomUUID();
      Content saved = makeContent(savedId, ContentType.MOVIE, "tmdb-001");

      given(contentRepository.save(any(Content.class)))
          .willReturn(saved);

      ContentResponse response = contentService.createContent(request, thumbnail);

      assertThat(response.id()).isEqualTo(savedId);
      assertThat(response.type()).isEqualTo(ContentType.MOVIE);
      assertThat(response.title()).isEqualTo("테스트 제목");

      ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
      then(contentRepository).should().save(captor.capture());
      assertThat(captor.getValue().getExternalId()).isNotBlank();
    }

    @Test
    @DisplayName("썸네일이 S3에 업로드되고, 그 URL이 콘텐츠에 반영된다")
    void success_uploadsThumbnailToS3() {
      ContentCreateRequest request = makeCreateRequest(ContentType.MOVIE);
      MultipartFile thumbnail = mock(MultipartFile.class);

      given(s3Service.upload(thumbnail)).willReturn("https://s3.example.com/thumb.jpg");
      given(contentRepository.save(any(Content.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      ContentResponse response = contentService.createContent(request, thumbnail);

      assertThat(response.thumbnailUrl()).isEqualTo("https://s3.example.com/thumb.jpg");
      then(s3Service).should().upload(thumbnail);
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
          ContentType.MOVIE, "수정된 제목", "수정된 설명", List.of("드라마")
      );
      MultipartFile thumbnail = mock(MultipartFile.class);

      given(contentRepository.findById(id)).willReturn(Optional.of(existing));
      given(contentRepository.save(any(Content.class))).willReturn(existing);

      ContentResponse response = contentService.updateContent(id, request, thumbnail);

      assertThat(response).isNotNull();
      ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
      then(contentRepository).should().save(captor.capture());
      assertThat(captor.getValue().getTitle()).isEqualTo("수정된 제목");
    }

    @Test
    @DisplayName("새 썸네일을 보내면 S3에 업로드되고, 그 URL로 갱신된다")
    void success_newThumbnail_uploadsToS3() {
      UUID id = UUID.randomUUID();
      Content existing = makeContent(id, ContentType.MOVIE, "tmdb-001");

      ContentUpdateRequest request = new ContentUpdateRequest(
          ContentType.MOVIE, "수정된 제목", "수정된 설명", List.of("드라마")
      );
      MultipartFile thumbnail = mock(MultipartFile.class);
      given(thumbnail.isEmpty()).willReturn(false);
      given(s3Service.upload(thumbnail)).willReturn("https://s3.example.com/new-thumb.jpg");

      given(contentRepository.findById(id)).willReturn(Optional.of(existing));
      given(contentRepository.save(any(Content.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      ContentResponse response = contentService.updateContent(id, request, thumbnail);

      assertThat(response.thumbnailUrl()).isEqualTo("https://s3.example.com/new-thumb.jpg");
      then(s3Service).should().upload(thumbnail);
    }

    @Test
    @DisplayName("썸네일을 안 보내면 S3 업로드 없이 기존 썸네일이 유지된다")
    void success_noThumbnail_keepsExisting() {
      UUID id = UUID.randomUUID();
      Content existing = makeContent(id, ContentType.MOVIE, "tmdb-001");

      ContentUpdateRequest request = new ContentUpdateRequest(
          ContentType.MOVIE, "수정된 제목", "수정된 설명", List.of("드라마")
      );

      given(contentRepository.findById(id)).willReturn(Optional.of(existing));
      given(contentRepository.save(any(Content.class)))
          .willAnswer(invocation -> invocation.getArgument(0));

      ContentResponse response = contentService.updateContent(id, request, null);

      assertThat(response.thumbnailUrl()).isEqualTo(existing.getThumbnailUrl());
      then(s3Service).should(org.mockito.Mockito.never()).upload(any());
    }

    @Test
    @DisplayName("관리자가 직접 등록한 콘텐츠는 유형도 변경 가능")
    void success_manuallyCreatedContent_typeChanges() {
      UUID id = UUID.randomUUID();
      Content existing = makeContent(id, ContentType.MOVIE, Content.MANUAL_EXTERNAL_ID_PREFIX + id);

      ContentUpdateRequest request = new ContentUpdateRequest(
          ContentType.TV_SERIES, "수정된 제목", "수정된 설명", List.of("드라마")
      );
      MultipartFile thumbnail = mock(MultipartFile.class);

      given(contentRepository.findById(id)).willReturn(Optional.of(existing));
      given(contentRepository.save(any(Content.class))).willReturn(existing);

      contentService.updateContent(id, request, thumbnail);

      ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
      then(contentRepository).should().save(captor.capture());
      assertThat(captor.getValue().getType()).isEqualTo(ContentType.TV_SERIES);
    }

    @Test
    @DisplayName("외부 수집 콘텐츠는 유형 변경 시도 시 예외 발생")
    void fail_typeChangeBlockedForExternalContent() {
      UUID id = UUID.randomUUID();
      Content existing = makeContent(id, ContentType.MOVIE, "tmdb-001");

      ContentUpdateRequest request = new ContentUpdateRequest(
          ContentType.TV_SERIES, "수정된 제목", "수정된 설명", List.of("드라마")
      );

      given(contentRepository.findById(id)).willReturn(Optional.of(existing));

      assertThatThrownBy(() ->
          contentService.updateContent(id, request, mock(MultipartFile.class)))
          .isInstanceOf(MoplException.class)
          .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
              .isEqualTo(ErrorCode.CONTENT_TYPE_NOT_EDITABLE));

      then(contentRepository).should(org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 콘텐츠 수정 - 예외 발생")
    void fail_notFound() {
      UUID id = UUID.randomUUID();
      given(contentRepository.findById(id)).willReturn(Optional.empty());

      ContentUpdateRequest request = new ContentUpdateRequest(
          ContentType.MOVIE, "수정된 제목", "수정된 설명", List.of()
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
    @DisplayName("정상 조회 - ContentResponse 반환, 시청자 수가 함께 채워진다")
    void success() {
      UUID id = UUID.randomUUID();
      Content content = makeContent(id, ContentType.TV_SERIES, "tmdb-002");
      given(contentRepository.findById(id)).willReturn(Optional.of(content));
      given(loadWatcherCountPort.countByContentId(id)).willReturn(7L);

      ContentResponse response = contentService.getContent(id);

      assertThat(response.id()).isEqualTo(id);
      assertThat(response.type()).isEqualTo(ContentType.TV_SERIES);
      assertThat(response.watcherCount()).isEqualTo(7L);
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
      given(loadWatcherCountPort.countByContentIds(anyCollection())).willReturn(Map.of());

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
      // second는 배치 조회 결과에 없는 경우(0으로 대체)까지 함께 검증
      given(loadWatcherCountPort.countByContentIds(anyCollection()))
          .willReturn(Map.of(first.getId(), 3L));

      // when
      CursorPageResponse<ContentResponse> response = contentService.getContents(request);

      // then
      assertThat(response.data()).hasSize(2); // limit만큼만 반환
      assertThat(response.hasNext()).isTrue();
      assertThat(response.nextCursor()).isEqualTo(second.getCreatedAt().toString());
      assertThat(response.nextIdAfter()).isEqualTo(second.getId());
      assertThat(response.totalCount()).isEqualTo(10L);
      assertThat(response.data().get(0).watcherCount()).isEqualTo(3L);
      assertThat(response.data().get(1).watcherCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("빈 결과 - data 비어있고 hasNext=false")
    void emptyResult() {
      // given
      ContentSearchRequest request = makeSearchRequest(20);
      given(contentRepository.findAllByCondition(request)).willReturn(List.of());
      given(contentRepository.countByCondition(request)).willReturn(0L);
      given(loadWatcherCountPort.countByContentIds(anyCollection())).willReturn(Map.of());

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
      given(loadWatcherCountPort.countByContentIds(anyCollection())).willReturn(Map.of());

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
      given(loadWatcherCountPort.countByContentIds(anyCollection())).willReturn(Map.of());

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
