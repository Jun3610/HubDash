package com.junyoung.dashboard.global.notion;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NotionIdsTest {

    @Test
    void extractsIdFromUrlsAndUuids() {
        String id = "66d2b9e1fd1482abb73a01f2f39c987c";
        assertThat(NotionIds.extract("https://app.notion.com/p/" + id)).isEqualTo(id);
        assertThat(NotionIds.extract("https://www.notion.so/Docker-" + id + "?v=3822b9e1fd1482a1b58b07634f10ff39")).isEqualTo(id);
        assertThat(NotionIds.extract("66d2b9e1-fd14-82ab-b73a-01f2f39c987c")).isEqualTo(id);
        assertThat(NotionIds.extract("  ")).isNull();
        assertThat(NotionIds.extract("https://example.com")).isNull();
    }

    @Test
    void readsTitleStatusDateByType() {
        Map<String, Object> row = Map.of(
                "id", "3532b9e1-fd14-80ff-b994-fe9eb920f8c3",
                "properties", Map.of(
                        "개념 ", Map.of("type", "title", "title", List.of(Map.of("plain_text", "도커 "), Map.of("plain_text", "개념"))),
                        "상태", Map.of("type", "status", "status", Map.of("name", "완료")),
                        "마감일", Map.of("type", "date", "date", Map.of("start", "2026-06-03"))));
        NotionPage p = NotionClient.toPage(row);
        assertThat(p.title()).isEqualTo("도커 개념");
        assertThat(p.status()).isEqualTo("완료");
        assertThat(p.date()).isEqualTo("2026-06-03");
    }
}
