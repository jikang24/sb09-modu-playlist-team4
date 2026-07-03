package com.mopl.domain.content.domain;

/**
 * 콘텐츠 목록 정렬 기준
 *
 * 프론트가 보내는 sortBy 문자열을 실제 정렬/커서 비교에 쓸 엔티티 필드로 매핑
 * (프론트 "인기순" 옵션은 watcherCount로 오지만, 실제 엔티티엔 reviewCount만 존재)
 */
public enum ContentSortField {
  CREATED_AT("createdAt"),
  REVIEW_COUNT("reviewCount");

  private final String propertyName;

  ContentSortField(String propertyName) {
    this.propertyName = propertyName;
  }

  public String propertyName() {
    return propertyName;
  }

  public static ContentSortField resolve(String sortBy) {
    if ("watcherCount".equals(sortBy) || "reviewCount".equals(sortBy)) {
      return REVIEW_COUNT;
    }
    return CREATED_AT;
  }
}