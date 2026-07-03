package com.mopl.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @RequestParam/@PathVariable으로 들어오는 enum 값의 대소문자/구분자를 구분하지 않고 바인딩
 *
 * 프론트가 소문자(movie)든 카멜케이스(tvSeries)든 상관없이
 * ContentType.MOVIE, ContentType.TV_SERIES 등으로 매칭되도록 함
 * (기본 StringToEnumConverterFactory는 완전 일치만 허용해서 IllegalArgumentException → 500 발생)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverterFactory(new CaseInsensitiveEnumConverterFactory());
  }

  private static class CaseInsensitiveEnumConverterFactory
      implements ConverterFactory<String, Enum> {

    @Override
    public <T extends Enum> Converter<String, T> getConverter(Class<T> targetType) {
      return source -> {
        if (source.isBlank()) {
          return null;
        }
        String normalizedSource = normalize(source);
        for (T constant : targetType.getEnumConstants()) {
          if (normalize(constant.name()).equals(normalizedSource)) {
            return constant;
          }
        }
        throw new IllegalArgumentException(
            "No enum constant " + targetType.getName() + " for value [" + source + "]");
      };
    }

    // 대소문자 + '_' 구분 없이 비교 (movie == MOVIE, tvSeries == TV_SERIES 모두 매칭)
    private String normalize(String value) {
      return value.replace("_", "").toUpperCase();
    }
  }
}