package com.mopl.global.config;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PlaceholderImageControllerTest {

  private final MockMvc mockMvc =
      MockMvcBuilders.standaloneSetup(new PlaceholderImageController()).build();

  @Test
  @DisplayName("/placeholder-movie.png 요청 - JSON 404 대신 fallback SVG를 200으로 반환한다")
  void placeholderMovie_returnsFallbackSvg() throws Exception {
    mockMvc.perform(get("/placeholder-movie.png"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("image/svg+xml"))
        .andExpect(content().string(containsString("<svg")));
  }
}