package com.junyoung.dashboard.domain.notion.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.junyoung.dashboard.domain.notion.NotionIds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

// Notion 공개 API(https://developers.notion.com) 호출. 노션 "내부 통합(integration)" 토큰을 쓰고,
// 통합에 공유된 페이지/DB만 보인다 — 동기화할 DB/페이지는 노션에서 이 통합을 연결(Connections)해 둬야 한다.
@Component
public class NotionApiClient implements NotionClient {

    // databases/{id}/query를 그대로 쓰는 마지막 버전. 이후 버전은 DB 조회가 data source 단위로 바뀐다.
    static final String NOTION_VERSION = "2022-06-28";
    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;
    private final boolean configured;

    public NotionApiClient(RestClient.Builder builder,
                           @Value("${app.notion.token:}") String token,
                           @Value("${app.notion.base-url:https://api.notion.com}") String baseUrl) {
        this.configured = token != null && !token.isBlank();
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader("Notion-Version", NOTION_VERSION)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return configured;
    }

    @Override
    public List<NotionPage> listDatabasePages(String databaseId) {
        return collectAll(cursor -> {
            Map<String, Object> body = new HashMap<>();
            body.put("page_size", PAGE_SIZE);
            if (cursor != null) {
                body.put("start_cursor", cursor);
            }
            ListResponse response = restClient.post()
                    .uri("/v1/databases/{id}/query", databaseId)
                    .body(body)
                    .retrieve()
                    .body(ListResponse.class);
            List<NotionPage> pages = new ArrayList<>();
            for (Item item : response.results()) {
                if ("page".equals(item.object()) && !item.removed()) {
                    pages.add(new NotionPage(normalize(item.id()), item.databaseTitle()));
                }
            }
            return new Batch(pages, response);
        });
    }

    @Override
    public List<NotionPage> listChildPages(String pageId) {
        return collectAll(cursor -> {
            ListResponse response = restClient.get()
                    .uri(uri -> {
                        uri.path("/v1/blocks/{id}/children").queryParam("page_size", PAGE_SIZE);
                        if (cursor != null) {
                            uri.queryParam("start_cursor", cursor);
                        }
                        return uri.build(pageId);
                    })
                    .retrieve()
                    .body(ListResponse.class);
            List<NotionPage> pages = new ArrayList<>();
            for (Item item : response.results()) {
                if ("child_page".equals(item.type()) && item.childPage() != null && !item.removed()) {
                    pages.add(new NotionPage(normalize(item.id()), item.childPage().title()));
                }
            }
            return new Batch(pages, response);
        });
    }

    // 노션 목록 API는 최대 100건씩 주고 has_more/next_cursor로 다음 페이지를 알려준다.
    private List<NotionPage> collectAll(Function<String, Batch> fetch) {
        List<NotionPage> all = new ArrayList<>();
        String cursor = null;
        do {
            Batch batch = fetch.apply(cursor);
            all.addAll(batch.pages());
            cursor = Boolean.TRUE.equals(batch.response().hasMore()) ? batch.response().nextCursor() : null;
        } while (cursor != null);
        return all;
    }

    private static String normalize(String id) {
        return NotionIds.extract(id).orElse(id);
    }

    private record Batch(List<NotionPage> pages, ListResponse response) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ListResponse(
            List<Item> results,
            @JsonProperty("has_more") Boolean hasMore,
            @JsonProperty("next_cursor") String nextCursor
    ) {
    }

    // DB 조회 결과(page)와 블록 조회 결과(block)를 한 타입으로 받는다 — 필요한 필드만 매핑.
    // 블록 종류에 따라 없는 필드가 있어 Boolean으로 받는다(Jackson 3는 없는 값을 기본형 boolean에 넣지 못함).
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(
            String object,
            String id,
            String type,
            Boolean archived,
            @JsonProperty("in_trash") Boolean inTrash,
            Map<String, Property> properties,
            @JsonProperty("child_page") ChildPage childPage
    ) {
        boolean removed() {
            return Boolean.TRUE.equals(archived) || Boolean.TRUE.equals(inTrash);
        }

        // DB 행의 제목은 이름이 제각각인 "title" 타입 속성 하나에 들어 있다.
        String databaseTitle() {
            if (properties == null) {
                return "";
            }
            return properties.values().stream()
                    .filter(p -> "title".equals(p.type()) && p.title() != null)
                    .findFirst()
                    .map(p -> p.title().stream()
                            .map(RichText::plainText)
                            .filter(Objects::nonNull)
                            .collect(Collectors.joining()))
                    .orElse("");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Property(String type, List<RichText> title) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RichText(@JsonProperty("plain_text") String plainText) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChildPage(String title) {
    }
}
