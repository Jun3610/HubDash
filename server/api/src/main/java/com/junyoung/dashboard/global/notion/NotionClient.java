package com.junyoung.dashboard.global.notion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 노션 공개 API로 DB 행을 읽는다 (이슈 #163). 토큰은 환경변수 NOTION_TOKEN(레포 루트 .env, 커밋 안 함).
 * 읽을 DB는 노션에서 이 통합(integration)에 연결돼 있어야 한다.
 */
@Component
public class NotionClient {

    private static final String VERSION = "2022-06-28";

    private final String token;
    private final RestClient rest;

    public NotionClient(@Value("${app.notion.token:}") String token,
                        @Value("${app.notion.base-url:https://api.notion.com/v1}") String baseUrl) {
        this.token = token;
        this.rest = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Notion-Version", VERSION)
                .build();
    }

    public boolean configured() {
        return token != null && !token.isBlank();
    }

    /** DB의 모든 행 (100개씩 끝까지) */
    public List<NotionPage> queryDatabase(String databaseId) {
        if (!configured()) {
            throw new NotionException("NOTION_TOKEN이 설정되지 않았어요 (레포 루트 .env)");
        }
        List<NotionPage> pages = new ArrayList<>();
        String cursor = null;
        do {
            Map<String, Object> body = cursor == null ? Map.of("page_size", 100) : Map.of("page_size", 100, "start_cursor", cursor);
            Map<?, ?> res = rest.post()
                    .uri("/databases/{id}/query", databaseId)
                    .header("Authorization", "Bearer " + token)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, r) -> {
                        throw new NotionException(r.getStatusCode().value() == 404
                                ? "노션 DB(" + databaseId + ")를 읽을 수 없어요 — 노션에서 이 DB를 HubDash 통합에 연결해 주세요"
                                : "노션 API 오류 (" + r.getStatusCode().value() + ")");
                    })
                    .body(Map.class);
            if (res == null) {
                break;
            }
            for (Object row : (List<?>) res.get("results")) {
                pages.add(toPage((Map<?, ?>) row));
            }
            cursor = Boolean.TRUE.equals(res.get("has_more")) ? (String) res.get("next_cursor") : null;
        } while (cursor != null);
        return pages;
    }

    /** 제목 칸(title), 상태 칸(status/select), 날짜 칸(date)을 이름과 상관없이 종류로 찾는다 */
    static NotionPage toPage(Map<?, ?> row) {
        String title = "";
        String status = null;
        String date = null;
        Map<?, ?> props = (Map<?, ?>) row.get("properties");
        for (Object v : props.values()) {
            Map<?, ?> p = (Map<?, ?>) v;
            String type = String.valueOf(p.get("type"));
            switch (type) {
                case "title" -> title = plain((List<?>) p.get("title"));
                case "status", "select" -> {
                    Map<?, ?> s = (Map<?, ?>) p.get(type);
                    if (s != null && status == null) {
                        status = String.valueOf(s.get("name"));
                    }
                }
                case "date" -> {
                    Map<?, ?> d = (Map<?, ?>) p.get("date");
                    if (d != null && date == null) {
                        date = String.valueOf(d.get("start"));
                    }
                }
                default -> {
                }
            }
        }
        return new NotionPage(String.valueOf(row.get("id")), title.strip(), status, date);
    }

    private static String plain(List<?> rich) {
        StringBuilder sb = new StringBuilder();
        if (rich != null) {
            for (Object r : rich) {
                Object t = ((Map<?, ?>) r).get("plain_text");
                if (t != null) {
                    sb.append(t);
                }
            }
        }
        return sb.toString();
    }
}
