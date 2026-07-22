package com.mopl.domain.content.adapter.out.opensearch;

import com.mopl.domain.content.adapter.out.opensearch.document.ContentDocument;
import com.mopl.domain.content.adapter.out.opensearch.util.ChosungUtils;
import com.mopl.domain.content.adapter.port.SearchContentPort;
import com.mopl.domain.content.adapter.port.SearchContentResult;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentSortField;
import com.mopl.domain.content.dto.ContentSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.query_dsl.TextQueryType;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSearchAdapter implements SearchContentPort {

  private static final String INDEX = "contents";

  private final OpenSearchClient client;

  @Override
  public void save(Content content) {

    try {

      ContentDocument document = ContentDocument.from(content);

      IndexRequest<ContentDocument> request =
          new IndexRequest.Builder<ContentDocument>()
              .index(INDEX)
              .id(content.getId().toString())
              .document(document)
              .build();

      client.index(request);

    } catch (Exception e) {

      log.error("OpenSearch 저장 실패. id={}", content.getId(), e);

    }
  }

  @Override
  public void delete(UUID id) {

    try {

      DeleteRequest request =
          new DeleteRequest.Builder()
              .index(INDEX)
              .id(id.toString())
              .build();

      client.delete(request);

    } catch (Exception e) {

      log.error("OpenSearch 삭제 실패. id={}", id, e);

    }
  }

  @Override
  public SearchContentResult search(String keyword, ContentSearchRequest request) {

    try {

      ContentSortField sortField = ContentSortField.resolve(request.sortBy());
      boolean isAscending = "ASCENDING".equalsIgnoreCase(request.sortDirection());
      SortOrder sortOrder = isAscending ? SortOrder.Asc : SortOrder.Desc;
      String openSearchSortField = toOpenSearchField(sortField);
      boolean isChosungQuery = ChosungUtils.isChosungOnly(keyword);

      SearchResponse<ContentDocument> response =
          client.search(s -> s
                  .index(INDEX)
                  .size(request.limit() + 1) // DB(findAllByIdsWithCondition)와 동일하게 hasNext 판단용 limit+1개
                  .query(q -> q
                      .bool(b -> {

                        b.must(m -> m
                            .bool(inner -> {

                              // 초성 검색어("ㅂㄹㅍㅋ")는 titleChosung 필드로만 매칭
                              if (isChosungQuery) {
                                inner.should(sh -> sh.match(mt -> mt
                                    .field("titleChosung")
                                    .query(FieldValue.of(keyword))
                                ));
                                return inner;
                              }

                              // 1) 부분 포함 검색 (n-gram)
                              inner.should(sh -> sh.multiMatch(mm -> mm
                                  .query(keyword)
                                  .fields("title.ngram^2", "description", "tags")
                                  .type(TextQueryType.Phrase)
                              ));

                              // 2) 자동완성 (edge-ngram, prefix)
                              inner.should(sh -> sh.match(mt -> mt
                                  .field("title.edge")
                                  .query(FieldValue.of(keyword))
                              ));

                              // 3) 오타 허용 (fuzzy)
                              inner.should(sh -> sh.match(mt -> mt
                                  .field("title")
                                  .query(FieldValue.of(keyword))
                                  .fuzziness("AUTO")
                              ));

                              inner.minimumShouldMatch("1");

                              return inner;
                            })
                        );

                        if (request.typeEqual() != null) {
                          b.filter(f -> f.term(t -> t
                              .field("type")
                              .value(v -> v.stringValue(request.typeEqual().name()))
                          ));
                        }
                        if (request.tagsIn() != null && !request.tagsIn().isEmpty()) {
                          b.filter(f -> f.terms(t -> t
                              .field("tags.keyword")
                              .terms(ts -> ts.value(
                                  request.tagsIn().stream().map(FieldValue::of).toList()
                              ))
                          ));
                        }
                        return b;
                      })
                  )
                  .sort(so -> so.field(f -> f.field(openSearchSortField).order(sortOrder)))
                  .sort(so -> so.field(f -> f.field("id").order(isAscending ? SortOrder.Asc : SortOrder.Desc))),
              ContentDocument.class
          );

      log.info("OpenSearch hit size = {}", response.hits().hits().size());
      log.info("OpenSearch total count = {}",
          response.hits().total() == null
              ? 0
              : response.hits().total().value());

      List<UUID> ids =
          response.hits()
              .hits()
              .stream()
              .map(Hit::id)
              .filter(Objects::nonNull)
              .map(UUID::fromString)
              .toList();

      long totalCount =
          response.hits().total() == null
              ? ids.size()
              : response.hits().total().value();

      return new SearchContentResult(ids, totalCount);

    } catch (Exception e) {

      log.error("OpenSearch 검색 실패. keyword={}", keyword, e);

      return new SearchContentResult(List.of(), 0);

    }
  }

  /** DB 정렬 기준(ContentSortField)을 OpenSearch 문서 필드명으로 매핑 */
  private String toOpenSearchField(ContentSortField sortField) {
    return switch (sortField) {
      case CREATED_AT -> "createdAt";
      case REVIEW_COUNT -> "reviewCount";
      case AVERAGE_RATING -> "averageRating";
    };
  }
}