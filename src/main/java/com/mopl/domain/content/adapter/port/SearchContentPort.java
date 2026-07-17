package com.mopl.domain.content.adapter.port;

import com.mopl.domain.content.domain.Content;

import com.mopl.domain.content.dto.ContentSearchRequest;
import java.util.UUID;

public interface SearchContentPort {

  SearchContentResult search(
      String keyword,
      ContentSearchRequest request
  );

  void save(Content content);

  void delete(UUID id);
}