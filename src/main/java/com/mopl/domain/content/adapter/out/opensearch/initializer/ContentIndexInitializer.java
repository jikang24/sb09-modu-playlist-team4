package com.mopl.domain.content.adapter.out.opensearch.initializer;

import com.mopl.domain.content.adapter.port.SearchContentPort;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.repository.ContentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(
    name = "opensearch.init.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@RequiredArgsConstructor
public class ContentIndexInitializer {

  private static final String INDEX = "contents";

  private final OpenSearchClient client;
  private final ContentRepository contentRepository;
  private final SearchContentPort searchContentPort;

  @PostConstruct
  public void initialize() {

    try {

      boolean exists = client.indices()
          .exists(e -> e.index(INDEX))
          .value();

      if (!exists) {

        CreateIndexRequest request = new CreateIndexRequest.Builder()
            .index(INDEX)
            .mappings(m -> m
                .properties("id", p -> p.keyword(k -> k))
                .properties("title", p -> p.text(t -> t))
                .properties("description", p -> p.text(t -> t))
                .properties("tags", p -> p.text(t -> t
                    .fields("keyword", f -> f.keyword(k -> k))
                ))
                .properties("type", p -> p.keyword(k -> k))
                .properties("averageRating", p -> p.double_(d -> d))
                .properties("reviewCount", p -> p.integer(i -> i))
                .properties("createdAt", p -> p.date(d -> d))
            )
            .build();

        client.indices().create(request);

        log.info("OpenSearch index '{}' created.", INDEX);

        // 인덱스가 새로 만들어졌을 때만 초기 데이터 적재
        List<Content> contents = contentRepository.findAll();

        for (Content content : contents) {
          searchContentPort.save(content);
        }

        log.info("Indexed {} contents into OpenSearch.", contents.size());

      } else {

        log.info("OpenSearch index '{}' already exists. Skip indexing.", INDEX);
      }

    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize OpenSearch index.", e);
    }
  }
}