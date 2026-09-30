package com.junyoung.dashboard.global.notion;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 노션 공개 API로 DB 행을 읽는다 (이슈 #163). 토큰은 호출할 때마다 NotionTokenSource에서 읽는다(이슈 #227) —
 * 웹 설정에서 저장한 값이 먼저, 없으면 환경변수 NOTION_TOKEN.
 * 읽을 DB는 노션에서 이 통합(integration)에 연결돼 있어야 한다.
 */
@Component
public class NotionClient {

    private static final String VERSION = "2022-06-28";

    private final NotionTokenSource tokens;
    private final RestClient rest;

    // 생성자가 둘(테스트용 Builder 주입)이라 스프링이 쓸 생성자를 명시한다
    @Autowired
    public NotionClient(NotionTokenSource tokens,
                        @Value("${app.notion.base-url:https://api.notion.com/v1}") String baseUrl) {
        this(tokens, RestClient.builder().baseUrl(baseUrl));
    }

    NotionClient(NotionTokenSource tokens, RestClient.Builder builder) {
        this.tokens = tokens;
        this.rest = builder.defaultHeader("Notion-Version", VERSION).build();
    }

    public boolean configured() {
        String token = tokens.currentToken();
        return token != null && !token.isBlank();
    }

    /**
     * 토큰이 유효한지 확인하고 어떤 통합인지 돌려준다 (GET /users/me). 저장 전 검증용이라 저장된 토큰이 아니라 인자로 받는다.
     * 실패 문구에 토큰을 넣지 않는다.
     */
    public NotionAccount me(String token) {
        Map<?, ?> res = rest.get()
                .uri("/users/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, r) -> {
                    int code = r.getStatusCode().value();
                    throw new NotionException(code == 401
                            ? "노션 토큰이 올바르지 않아요 — 노션 통합(integration) 페이지에서 토큰을 다시 복사해 주세요"
                            : "노션 API 오류 (" + code + ")");
                })
                .body(Map.class);
        if (res == null) {
            throw new NotionException("노션 API 응답이 비어 있어요");
        }
        Object bot = res.get("bot");
        String workspace = bot instanceof Map<?, ?> b && b.get("workspace_name") != null
                ? String.valueOf(b.get("workspace_name")) : null;
        return new NotionAccount(String.valueOf(res.get("id")), workspace);
    }

    /** DB의 모든 행 (100개씩 끝까지) */
    public List<NotionPage> queryDatabase(String databaseId) {
        List<NotionPage> pages = new ArrayList<>();
        for (Map<?, ?> row : queryDatabaseRows(databaseId)) {
            pages.add(toPage(row));
        }
        return pages;
    }

    /** DB의 모든 행을 노션 응답 모양 그대로 (속성을 직접 읽어야 하는 일정 동기화용, 이슈 #228) */
    public List<Map<?, ?>> queryDatabaseRows(String databaseId) {
        String token = tokens.currentToken();
        if (token == null || token.isBlank()) {
            throw new NotionException("노션 토큰이 없어요 — 설정 화면에서 노션 토큰을 저장해 주세요");
        }
        List<Map<?, ?>> rows = new ArrayList<>();
        String cursor = null;
        do {
            Map<String, Object> body = cursor == null ? Map.of("page_size", 100) : Map.of("page_size", 100, "start_cursor", cursor);
            Map<?, ?> res = rest.post()
                    .uri("/databases/{id}/query", databaseId)
                    .header("Authorization", "Bearer " + token)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, r) -> {
                        int code = r.getStatusCode().value();
                        throw new NotionException(code == 404
                                ? "노션 DB(" + databaseId + ")를 읽을 수 없어요 — 노션에서 이 DB를 HubDash 통합에 연결해 주세요"
                                : code == 401
                                ? "노션 토큰이 올바르지 않아요 — 설정 화면에서 노션 토큰을 다시 저장해 주세요"
                                : "노션 API 오류 (" + code + ")");
                    })
                    .body(Map.class);
            if (res == null) {
                break;
            }
            for (Object row : (List<?>) res.get("results")) {
                rows.add((Map<?, ?>) row);
            }
            cursor = Boolean.TRUE.equals(res.get("has_more")) ? (String) res.get("next_cursor") : null;
        } while (cursor != null);
        return rows;
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
                case "title" -> title = plainText((List<?>) p.get("title"));
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

    /** 노션 rich text 배열의 글자만 이어 붙인다 */
    public static String plainText(List<?> rich) {
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
