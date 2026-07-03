package com.mopl.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @RequestParam/@PathVariable으로 들어오는 enum 값의 대소문자를 구분하지 않고 바인딩
 *
 * 프론트가 소문자(예: type=movie)로 보내도 ContentType.MOVIE 등으로 매칭되도록 함
 * (기본 StringToEnumConverterFactory는 대소문자를 구분해서 IllegalArgumentException → 500 발생)
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
        return (T) Enum.valueOf((Class<? extends Enum>) targetType, source.trim().toUpperCase());
      };
    }
  }
}