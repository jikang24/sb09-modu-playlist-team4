package com.mopl.domain.content.adapter.out.opensearch.util;

public final class ChosungUtils {

  private static final char[] CHOSUNG = {
      'ㄱ','ㄲ','ㄴ','ㄷ','ㄸ','ㄹ','ㅁ','ㅂ','ㅃ',
      'ㅅ','ㅆ','ㅇ','ㅈ','ㅉ','ㅊ','ㅋ','ㅌ','ㅍ','ㅎ'
  };

  private ChosungUtils() {}

  /** 완성형 한글 문자열에서 초성만 추출. 한글이 아닌 문자는 그대로 유지. */
  public static String extract(String input) {
    if (input == null || input.isBlank()) {
      return "";
    }

    StringBuilder sb = new StringBuilder();

    for (char ch : input.toCharArray()) {
      if (ch >= 0xAC00 && ch <= 0xD7A3) {
        int index = (ch - 0xAC00) / (21 * 28);
        sb.append(CHOSUNG[index]);
      } else {
        sb.append(ch);
      }
    }

    return sb.toString();
  }

  /** 검색어가 초성으로만 구성됐는지 판별 (예: "ㅂㄹㅍㅋ") */
  public static boolean isChosungOnly(String input) {
    return input != null && input.matches("^[ㄱ-ㅎ]+$");
  }
}