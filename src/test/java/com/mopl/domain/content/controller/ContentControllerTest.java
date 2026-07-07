package com.mopl.domain.content.controller;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentCreateRequest;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.dto.ContentSearchRequest;
import com.mopl.domain.content.dto.ContentUpdateRequest;
import com.mopl.domain.content.service.ContentUseCase;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.response.CursorPageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

@DisplayName("ContentController 테스트")
class ContentControllerTest {

    private ContentUseCase contentUseCase;
    private ContentController contentController;

    private ContentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        contentUseCase = mock(ContentUseCase.class);
        contentController = new ContentController(contentUseCase);

        sampleResponse = new ContentResponse(
            UUID.randomUUID(), ContentType.MOVIE,
            "테스트 제목", "테스트 설명", null,
            List.of("액션"), BigDecimal.ZERO, 0
        );
    }

    @Nested
    @DisplayName("콘텐츠 등록 - POST /api/contents")
    class CreateContent {

        @Test
        @DisplayName("정상 등록 - 201 반환")
        void success() {
            ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "제목", "설명", List.of());
            MultipartFile thumbnail = mock(MultipartFile.class);

            given(contentUseCase.createContent(request, thumbnail)).willReturn(sampleResponse);

            ResponseEntity<ContentResponse> response =
                contentController.createContent(request, thumbnail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isEqualTo(sampleResponse);
            then(contentUseCase).should().createContent(request, thumbnail);
        }

        @Test
        @DisplayName("등록 실패 - MoplException 전파")
        void fail_propagatesException() {
            ContentCreateRequest request = new ContentCreateRequest(
                ContentType.MOVIE, "제목", "설명", List.of());
            MultipartFile thumbnail = mock(MultipartFile.class);

            given(contentUseCase.createContent(request, thumbnail))
                .willThrow(new MoplException(ErrorCode.INVALID_INPUT));

            assertThatThrownBy(() -> contentController.createContent(request, thumbnail))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT));
        }
    }

    @Nested
    @DisplayName("콘텐츠 수정 - PATCH /api/contents/{id}")
    class UpdateContent {

        @Test
        @DisplayName("정상 수정 - 200 반환")
        void success() {
            UUID id = sampleResponse.id();
            ContentUpdateRequest request = new ContentUpdateRequest("새 제목", "새 설명", List.of());
            MultipartFile thumbnail = mock(MultipartFile.class);

            given(contentUseCase.updateContent(id, request, thumbnail)).willReturn(sampleResponse);

            ResponseEntity<ContentResponse> response =
                contentController.updateContent(id, request, thumbnail);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(sampleResponse);
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 수정 - MoplException 전파")
        void fail_notFound() {
            UUID id = UUID.randomUUID();
            ContentUpdateRequest request = new ContentUpdateRequest("새 제목", null, List.of());

            given(contentUseCase.updateContent(eq(id), eq(request), any()))
                .willThrow(new MoplException(ErrorCode.CONTENT_NOT_FOUND));

            assertThatThrownBy(() -> contentController.updateContent(id, request, null))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("콘텐츠 삭제 - DELETE /api/contents/{id}")
    class DeleteContent {

        @Test
        @DisplayName("정상 삭제 - 204 No Content 반환")
        void success() {
            UUID id = UUID.randomUUID();

            ResponseEntity<Void> response = contentController.deleteContent(id);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            then(contentUseCase).should().deleteContent(id);
        }

        @Test
        @DisplayName("존재하지 않는 콘텐츠 삭제 - MoplException 전파")
        void fail_notFound() {
            UUID id = UUID.randomUUID();

            doThrow(new MoplException(ErrorCode.CONTENT_NOT_FOUND))
                .when(contentUseCase).deleteContent(id);

            assertThatThrownBy(() -> contentController.deleteContent(id))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("콘텐츠 단건 조회 - GET /api/contents/{id}")
    class GetContent {

        @Test
        @DisplayName("정상 조회 - 200 반환, 응답 포함")
        void success() {
            UUID id = sampleResponse.id();
            given(contentUseCase.getContent(id)).willReturn(sampleResponse);

            ResponseEntity<ContentResponse> response =
                contentController.getContent(id);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().id()).isEqualTo(id);
        }

        @Test
        @DisplayName("존재하지 않는 ID - MoplException 전파")
        void fail_notFound() {
            UUID id = UUID.randomUUID();
            given(contentUseCase.getContent(id))
                .willThrow(new MoplException(ErrorCode.CONTENT_NOT_FOUND));

            assertThatThrownBy(() -> contentController.getContent(id))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("콘텐츠 목록 조회 - GET /api/contents")
    class GetContents {

        @Test
        @DisplayName("정상 조회 - 200 반환, 데이터 포함")
        void success() {
            CursorPageResponse<ContentResponse> pageResponse = new CursorPageResponse<>(
                List.of(sampleResponse), null, null,
                false, 1L, "createdAt", "DESCENDING"
            );

            given(contentUseCase.getContents(any(ContentSearchRequest.class)))
                .willReturn(pageResponse);

            ResponseEntity<CursorPageResponse<ContentResponse>> response =
                contentController.getContents(
                    ContentType.MOVIE, null, null,
                    null, null, 10,
                    "createdAt", "DESCENDING"
                );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().data()).hasSize(1);
        }

        @Test
        @DisplayName("필터 없이 조회 - ContentSearchRequest 파라미터 조합 전달")
        void success_noFilter() {
            CursorPageResponse<ContentResponse> pageResponse = new CursorPageResponse<>(
                List.of(), null, null, false, 0L, "title", "ASCENDING"
            );

            given(contentUseCase.getContents(any(ContentSearchRequest.class)))
                .willReturn(pageResponse);

            ResponseEntity<CursorPageResponse<ContentResponse>> response =
                contentController.getContents(
                    null, null, null,
                    null, null, 20,
                    "title", "ASCENDING"
                );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            then(contentUseCase).should().getContents(any(ContentSearchRequest.class));
        }
    }
}
