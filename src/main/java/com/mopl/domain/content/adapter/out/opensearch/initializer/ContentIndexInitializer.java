package com.mopl.domain.content.adapter.out.opensearch.initializer;

import com.mopl.domain.content.adapter.port.SearchContentPort;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.repository.ContentRepository;
import jakarta.annotation.PostConstruct;
import jakarta.json.stream.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

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

  private static final String SETTINGS_PATH = "opensearch/contents-settings.json";
  private static final String MAPPINGS_PATH = "opensearch/contents-mappings.json";

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

        JsonpMapper mapper = client._transport().jsonpMapper();

        IndexSettings settings = parse(mapper, SETTINGS_PATH, IndexSettings._DESERIALIZER);
        TypeMapping mappings = parse(mapper, MAPPINGS_PATH, TypeMapping._DESERIALIZER);

        CreateIndexRequest request = CreateIndexRequest.of(b -> b
            .index(INDEX)
            .settings(settings)
            .mappings(mappings));

        client.indices().create(request);

        log.info("OpenSearch index '{}' created.", INDEX);

        List<Content> contents = contentRepository.findAll();

        for (Content content : contents) {
          searchContentPort.save(content);
        }

        log.info("Indexed {} contents into OpenSearch.", contents.size());

      } else {

        log.info("OpenSearch index '{}' already exists. Skip indexing.", INDEX);
      }

    } catch (Exception e) {

      log.error("Failed to initialize OpenSearch index.", e);

    }
  }

  private <T> T parse(
      JsonpMapper mapper,
      String resourcePath,
      org.opensearch.client.json.JsonpDeserializer<T> deserializer) throws IOException {

    try (InputStream is = new ClassPathResource(resourcePath).getInputStream();
        JsonParser parser = mapper.jsonProvider().createParser(is)) {

      return deserializer.deserialize(parser, mapper);
    }
  }
}