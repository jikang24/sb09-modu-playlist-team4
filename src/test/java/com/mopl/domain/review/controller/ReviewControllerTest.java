package com.mopl.domain.review.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.review.dto.ReviewCreateRequest;
import com.mopl.domain.review.dto.ReviewDto;
import com.mopl.domain.review.dto.ReviewUpdateRequest;
import com.mopl.domain.review.service.ReviewService;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.GlobalExceptionHandler;
import com.mopl.global.exception.MoplException;
import com.mopl.global.jwt.JwtClaims;
import com.mopl.global.response.CursorPageResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewController 테스트")
class ReviewControllerTest {

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private ReviewService reviewService;

  private UUID userId;

  @BeforeEach
  void setUp() {
    userId = UUID.randomUUID();
    JwtClaims claims = JwtClaims.builder().userId(userId).build();

    // 컨트롤러가 @AuthenticationPrincipal JwtClaims로 인증된 사용자를 받으므로,
    // standalone MockMvc에서는 Security 전체를 띄우는 대신 이 파라미터만 고정값으로 채워준다.
    mockMvc = MockMvcBuilders.standaloneSetup(new ReviewController(reviewService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
          @Override
          public boolean supportsParameter(MethodParameter parameter) {
            return parameter.getParameterType().equals(JwtClaims.class);
          }

          @Override
          public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
              NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
            return claims;
          }
        })
        .build();
  }

  @Nested
  @DisplayName("POST /api/reviews")
  class CreateReview {

    @Test
    @DisplayName("성공: 리뷰를 생성하고 201과 생성된 id를 반환한다")
    void success() throws Exception {
      UUID contentId = UUID.randomUUID();
      UUID reviewId = UUID.randomUUID();
      ReviewCreateRequest request = new ReviewCreateRequest(contentId, "좋아요", 4.5);

      given(reviewService.createReview(eq(contentId), eq(userId), eq(BigDecimal.valueOf(4.5)), eq("좋아요")))
          .willReturn(reviewId);

      mockMvc.perform(post("/api/reviews")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(content().string("\"" + reviewId + "\""));
    }

    @Test
    @DisplayName("실패: 이미 리뷰를 작성했으면 409 Conflict")
    void fail_alreadyExists() throws Exception {
      ReviewCreateRequest request = new ReviewCreateRequest(UUID.randomUUID(), "좋아요", 4.0);

      given(reviewService.createReview(any(), eq(userId), any(), any()))
          .willThrow(new MoplException(ErrorCode.REVIEW_ALREADY_EXISTS));

      mockMvc.perform(post("/api/reviews")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("실패: rating이 유효 범위를 벗어나면 400 Bad Request (Bean Validation이 서비스 호출 전에 막는다)")
    void fail_invalidRating() throws Exception {
      ReviewCreateRequest request = new ReviewCreateRequest(UUID.randomUUID(), "좋아요", 10.0);

      mockMvc.perform(post("/api/reviews")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("PATCH /api/reviews/{reviewId}")
  class UpdateReview {

    @Test
    @DisplayName("성공: 리뷰를 수정하면 204 No Content")
    void success() throws Exception {
      UUID reviewId = UUID.randomUUID();
      ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 3.5);

      mockMvc.perform(patch("/api/reviews/" + reviewId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNoContent());

      verify(reviewService).updateReview(eq(reviewId), eq(userId), eq(BigDecimal.valueOf(3.5)), eq("수정된 리뷰"));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 리뷰면 404 Not Found")
    void fail_notFound() throws Exception {
      UUID reviewId = UUID.randomUUID();
      ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 3.5);

      doThrow(new MoplException(ErrorCode.REVIEW_NOT_FOUND))
          .when(reviewService).updateReview(eq(reviewId), eq(userId), any(), any());

      mockMvc.perform(patch("/api/reviews/" + reviewId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("실패: 작성자가 아니면 403 Forbidden")
    void fail_notOwner() throws Exception {
      UUID reviewId = UUID.randomUUID();
      ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 리뷰", 3.5);

      doThrow(new MoplException(ErrorCode.REVIEW_NOT_OWNER))
          .when(reviewService).updateReview(eq(reviewId), eq(userId), any(), any());

      mockMvc.perform(patch("/api/reviews/" + reviewId)
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("DELETE /api/reviews/{reviewId}")
  class DeleteReview {

    @Test
    @DisplayName("성공: 리뷰를 삭제하면 204 No Content")
    void success() throws Exception {
      UUID reviewId = UUID.randomUUID();

      mockMvc.perform(delete("/api/reviews/" + reviewId))
          .andExpect(status().isNoContent());

      verify(reviewService).deleteReview(eq(reviewId), eq(userId));
    }

    @Test
    @DisplayName("실패: 존재하지 않는 리뷰면 404 Not Found")
    void fail_notFound() throws Exception {
      UUID reviewId = UUID.randomUUID();
      doThrow(new MoplException(ErrorCode.REVIEW_NOT_FOUND))
          .when(reviewService).deleteReview(eq(reviewId), eq(userId));

      mockMvc.perform(delete("/api/reviews/" + reviewId))
          .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("실패: 작성자가 아니면 403 Forbidden")
    void fail_notOwner() throws Exception {
      UUID reviewId = UUID.randomUUID();
      doThrow(new MoplException(ErrorCode.REVIEW_NOT_OWNER))
          .when(reviewService).deleteReview(eq(reviewId), eq(userId));

      mockMvc.perform(delete("/api/reviews/" + reviewId))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/reviews")
  class GetReviews {

    @Test
    @DisplayName("성공: 리뷰 목록을 조회한다")
    void success() throws Exception {
      UUID contentId = UUID.randomUUID();
      ReviewDto reviewDto = new ReviewDto(
          UUID.randomUUID(), contentId,
          new ReviewDto.AuthorDto(UUID.randomUUID(), "작성자", "https://image.jpg"),
          "좋아요", BigDecimal.valueOf(4.5));

      CursorPageResponse<ReviewDto> response = new CursorPageResponse<>(
          List.of(reviewDto), null, null, false, 1L, "CREATED_AT", "DESCENDING");

      given(reviewService.getReviews(any())).willReturn(response);

      mockMvc.perform(get("/api/reviews")
              .param("contentId", contentId.toString())
              .param("limit", "10")
              .param("sortDirection", "DESCENDING")
              .param("sortBy", "CREATED_AT"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(1)))
          .andExpect(jsonPath("$.data[0].text").value("좋아요"))
          .andExpect(jsonPath("$.data[0].author.name").value("작성자"))
          .andExpect(jsonPath("$.hasNext").value(false))
          .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    @DisplayName("실패: 잘못된 커서 형식이면 400 Bad Request")
    void fail_invalidCursor() throws Exception {
      given(reviewService.getReviews(any()))
          .willThrow(new MoplException(ErrorCode.INVALID_CURSOR_FORMAT));

      mockMvc.perform(get("/api/reviews")
              .param("contentId", UUID.randomUUID().toString())
              .param("limit", "10")
              .param("sortDirection", "DESCENDING")
              .param("sortBy", "CREATED_AT")
              .param("cursor", "bad-cursor")
              .param("idAfter", UUID.randomUUID().toString()))
          .andExpect(status().isBadRequest());
    }
  }
}
