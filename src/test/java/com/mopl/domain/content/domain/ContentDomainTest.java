package com.mopl.domain.content.domain;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Content 도메인")
class ContentDomainTest {

    @Nested
    @DisplayName("신규 생성 - create()")
    class Create {

        @Test
        @DisplayName("정상 생성 - 초기값(averageRating=0, reviewCount=0) 설정")
        void success() {
            Content content = Content.create(
                ContentType.MOVIE, "tmdb-001",
                "제목", "설명", "https://thumb.jpg",
                List.of("액션", "SF")
            );

            assertThat(content.getId()).isNotNull();
            assertThat(content.getType()).isEqualTo(ContentType.MOVIE);
            assertThat(content.getExternalId()).isEqualTo("tmdb-001");
            assertThat(content.getTitle()).isEqualTo("제목");
            assertThat(content.getDescription()).isEqualTo("설명");
            assertThat(content.getThumbnailUrl()).isEqualTo("https://thumb.jpg");
            assertThat(content.getAverageRating()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(content.getReviewCount()).isZero();
            assertThat(content.getTags()).containsExactly("액션", "SF");
            assertThat(content.getCreatedAt()).isNotNull();
            assertThat(content.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("null 타입 - INVALID_INPUT 예외")
        void fail_nullType() {
            assertThatThrownBy(() ->
                Content.create(null, "tmdb-001", "제목", "설명", null, List.of()))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("null externalId - INVALID_INPUT 예외")
        void fail_nullExternalId() {
            assertThatThrownBy(() ->
                Content.create(ContentType.MOVIE, null, "제목", "설명", null, List.of()))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("공백 externalId - INVALID_INPUT 예외")
        void fail_blankExternalId() {
            assertThatThrownBy(() ->
                Content.create(ContentType.MOVIE, "  ", "제목", "설명", null, List.of()))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("null 제목 - INVALID_INPUT 예외")
        void fail_nullTitle() {
            assertThatThrownBy(() ->
                Content.create(ContentType.MOVIE, "tmdb-001", null, "설명", null, List.of()))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("공백 제목 - INVALID_INPUT 예외")
        void fail_blankTitle() {
            assertThatThrownBy(() ->
                Content.create(ContentType.MOVIE, "tmdb-001", "  ", "설명", null, List.of()))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("null 태그 - 빈 리스트로 초기화")
        void nullTags_becomesEmptyList() {
            Content content = Content.create(
                ContentType.TV_SERIES, "tmdb-002", "제목", null, null, null);

            assertThat(content.getTags()).isEmpty();
        }
    }

    @Nested
    @DisplayName("DB 복원 - restore()")
    class Restore {

        @Test
        @DisplayName("모든 필드가 그대로 복원된다")
        void success() {
            UUID id = UUID.randomUUID();
            Instant now = Instant.now();
            BigDecimal rating = new BigDecimal("4.50");

            Content content = Content.restore(
                id, ContentType.SPORT, "sports-001",
                "스포츠 제목", "설명", "https://thumb.jpg",
                rating, 10,
                now, now, List.of("스포츠")
            );

            assertThat(content.getId()).isEqualTo(id);
            assertThat(content.getType()).isEqualTo(ContentType.SPORT);
            assertThat(content.getExternalId()).isEqualTo("sports-001");
            assertThat(content.getTitle()).isEqualTo("스포츠 제목");
            assertThat(content.getAverageRating()).isEqualByComparingTo(rating);
            assertThat(content.getReviewCount()).isEqualTo(10);
            assertThat(content.getCreatedAt()).isEqualTo(now);
            assertThat(content.getTags()).containsExactly("스포츠");
        }
    }

    @Nested
    @DisplayName("정보 수정 - update()")
    class Update {

        @Test
        @DisplayName("관리자 등록 콘텐츠는 유형/제목/설명/썸네일/태그가 변경된다")
        void success() {
            Content content = Content.create(
                ContentType.MOVIE, Content.MANUAL_EXTERNAL_ID_PREFIX + "1",
                "원래 제목", "원래 설명", null, List.of("액션"));

            content.update(ContentType.TV_SERIES, "새 제목", "새 설명", "https://new.jpg",
                List.of("드라마", "로맨스"));

            assertThat(content.getType()).isEqualTo(ContentType.TV_SERIES);
            assertThat(content.getTitle()).isEqualTo("새 제목");
            assertThat(content.getDescription()).isEqualTo("새 설명");
            assertThat(content.getThumbnailUrl()).isEqualTo("https://new.jpg");
            assertThat(content.getTags()).containsExactly("드라마", "로맨스");
        }

        @Test
        @DisplayName("외부 수집 콘텐츠는 유형을 바꾸려 하면 예외 발생")
        void fail_typeChangeBlockedForExternalContent() {
            Content content = Content.create(
                ContentType.MOVIE, "tmdb-001", "원래 제목", "원래 설명", null, List.of("액션"));

            assertThatThrownBy(() -> content.update(
                ContentType.TV_SERIES, "새 제목", "새 설명", "https://new.jpg", List.of("드라마")))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CONTENT_TYPE_NOT_EDITABLE));

            assertThat(content.getType()).isEqualTo(ContentType.MOVIE);
        }

        @Test
        @DisplayName("외부 수집 콘텐츠도 유형을 그대로 유지하면 수정 가능")
        void success_externalContent_sameTypeUnchanged() {
            Content content = Content.create(
                ContentType.MOVIE, "tmdb-001", "원래 제목", "원래 설명", null, List.of("액션"));

            content.update(ContentType.MOVIE, "새 제목", "새 설명", "https://new.jpg", List.of("드라마"));

            assertThat(content.getType()).isEqualTo(ContentType.MOVIE);
            assertThat(content.getTitle()).isEqualTo("새 제목");
        }

        @Test
        @DisplayName("null 태그로 수정 시 빈 리스트로 변경")
        void nullTags_becomesEmptyList() {
            Content content = Content.create(
                ContentType.MOVIE, "tmdb-001", "제목", null, null, List.of("액션"));

            content.update(ContentType.MOVIE, "제목", null, null, null);

            assertThat(content.getTags()).isEmpty();
        }

        @Test
        @DisplayName("updatedAt이 갱신된다")
        void updatedAt_isRefreshed() throws InterruptedException {
            Content content = Content.create(
                ContentType.MOVIE, "tmdb-001", "제목", null, null, List.of());
            Instant before = content.getUpdatedAt();

            Thread.sleep(5);
            content.update(ContentType.MOVIE, "새 제목", null, null, List.of());

            assertThat(content.getUpdatedAt()).isAfter(before);
        }

        @Test
        @DisplayName("type이 null이면 예외 발생")
        void nullType_throwsException() {
            Content content = Content.create(
                ContentType.MOVIE, "tmdb-001", "제목", null, null, List.of());

            assertThatThrownBy(() -> content.update(null, "새 제목", null, null, List.of()))
                .isInstanceOf(MoplException.class)
                .satisfies(e -> assertThat(((MoplException) e).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_INPUT));
        }
    }

    @Nested
    @DisplayName("평점 갱신 - updateRatingStats()")
    class UpdateRatingStats {

        @Test
        @DisplayName("averageRating과 reviewCount가 갱신된다")
        void success() {
            Content content = Content.create(
                ContentType.MOVIE, "tmdb-001", "제목", null, null, List.of());

            content.updateRatingStats(new BigDecimal("4.50"), 20);

            assertThat(content.getAverageRating()).isEqualByComparingTo(new BigDecimal("4.50"));
            assertThat(content.getReviewCount()).isEqualTo(20);
        }

        @Test
        @DisplayName("updatedAt이 갱신된다")
        void updatedAt_isRefreshed() throws InterruptedException {
            Content content = Content.create(
                ContentType.MOVIE, "tmdb-001", "제목", null, null, List.of());
            Instant before = content.getUpdatedAt();

            Thread.sleep(5);
            content.updateRatingStats(new BigDecimal("3.00"), 5);

            assertThat(content.getUpdatedAt()).isAfter(before);
        }
    }
}
