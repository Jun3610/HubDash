package com.junyoung.dashboard.domain.health.external.fatsecret;

import com.junyoung.dashboard.domain.health.dto.FoodDetailResponse;
import com.junyoung.dashboard.domain.health.dto.FoodSearchItemResponse;
import com.junyoung.dashboard.domain.health.dto.FoodSearchResponse;
import com.junyoung.dashboard.domain.health.dto.FoodServingResponse;
import com.junyoung.dashboard.domain.health.external.fatsecret.FatSecretException.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

// FatSecret Platform API(foods.search / food.get.v4) 클라이언트.
// FatSecret은 실패도 HTTP 200 + {"error":{"code","message"}} 본문으로 돌려주는 경우가 많아(실제 호출로 확인:
// 토큰 오류 code 13, IP 미허용 code 21) HTTP 상태와 본문의 error 필드를 둘 다 검사한다.
@Component
public class FatSecretClient {

    private static final Logger log = LoggerFactory.getLogger(FatSecretClient.class);

    private static final int CODE_INVALID_TOKEN = 13;
    private static final int CODE_IP_NOT_ALLOWED = 21;
    // 실제 호출로 확인: food_id=abc → 105(Invalid long value), 존재하지 않는 숫자 ID → 106(Invalid ID)
    private static final int CODE_INVALID_VALUE = 105;
    private static final int CODE_INVALID_ID = 106;

    private final FatSecretProperties properties;
    private final FatSecretTokenProvider tokenProvider;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FatSecretClient(FatSecretProperties properties, FatSecretTokenProvider tokenProvider,
                            RestClient fatSecretRestClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.tokenProvider = tokenProvider;
        this.restClient = fatSecretRestClient;
        this.objectMapper = objectMapper;
    }

    public FoodSearchResponse searchFoods(String query, int page, int size) {
        JsonNode root = call(UriComponentsBuilder.fromUriString(properties.apiUrl())
                .queryParam("method", "foods.search")
                .queryParam("search_expression", "{q}")
                .queryParam("page_number", page)
                .queryParam("max_results", size)
                .queryParam("format", "json")
                .build(false).toUriString(), query);

        JsonNode foods = root.path("foods");
        List<FoodSearchItemResponse> items = new ArrayList<>();
        for (JsonNode food : asList(foods.get("food"))) {
            items.add(new FoodSearchItemResponse(
                    text(food, "food_id"), text(food, "food_name"), text(food, "brand_name"),
                    text(food, "food_type"), text(food, "food_description")));
        }
        return new FoodSearchResponse(items, page, size, Math.max(0, longValue(foods, "total_results", items.size())));
    }

    public FoodDetailResponse getFood(String foodId) {
        JsonNode root = call(UriComponentsBuilder.fromUriString(properties.apiUrl())
                .queryParam("method", "food.get.v4")
                .queryParam("food_id", "{q}")
                .queryParam("format", "json")
                .build(false).toUriString(), foodId);

        JsonNode food = root.get("food");
        if (food == null || food.isNull()) {
            throw new FatSecretException(Reason.UPSTREAM_ERROR, "FatSecret 응답에 food 항목이 없습니다");
        }
        List<FoodServingResponse> servings = new ArrayList<>();
        for (JsonNode serving : asList(food.path("servings").get("serving"))) {
            Double calories = doubleValue(serving, "calories");
            servings.add(new FoodServingResponse(
                    text(serving, "serving_id"), text(serving, "serving_description"),
                    doubleValue(serving, "metric_serving_amount"), text(serving, "metric_serving_unit"),
                    calories == null ? null : (int) Math.round(calories),
                    doubleValue(serving, "carbohydrate"), doubleValue(serving, "protein"),
                    doubleValue(serving, "fat"), doubleValue(serving, "sodium")));
        }
        return new FoodDetailResponse(text(food, "food_id"), text(food, "food_name"), text(food, "brand_name"),
                servings);
    }

    // 토큰이 거부되면(code 13/HTTP 401) 캐시를 비우고 한 번만 재발급해서 재시도한다.
    private JsonNode call(String uriTemplate, String queryValue) {
        for (int attempt = 0; ; attempt++) {
            JsonNode root = fetch(uriTemplate, queryValue);
            JsonNode error = root.get("error");
            if (error == null || error.isNull()) {
                return root;
            }
            int code = (int) error.path("code").asLong(-1);
            String message = error.path("message").asString("");
            if (code == CODE_INVALID_TOKEN && attempt == 0) {
                log.warn("FatSecret이 토큰을 거부해 재발급 후 재시도합니다: {}", message);
                tokenProvider.invalidate();
                continue;
            }
            throw toException(code, message);
        }
    }

    private JsonNode fetch(String uriTemplate, String queryValue) {
        String body;
        try {
            body = restClient.get()
                    .uri(uriTemplate, queryValue)
                    .headers(headers -> headers.setBearerAuth(tokenProvider.getToken()))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 429) {
                throw new FatSecretException(Reason.RATE_LIMITED, "FatSecret 호출 한도를 초과했습니다", e);
            }
            if (status == 401) {
                tokenProvider.invalidate();
                throw new FatSecretException(Reason.AUTH_FAILED, "FatSecret이 인증을 거부했습니다", e);
            }
            throw new FatSecretException(Reason.UPSTREAM_ERROR, "FatSecret 호출 실패: HTTP " + status, e);
        } catch (RestClientException e) {
            throw new FatSecretException(Reason.UPSTREAM_ERROR, "FatSecret 호출 중 네트워크 오류가 발생했습니다", e);
        }
        try {
            return objectMapper.readTree(body);
        } catch (JacksonException e) {
            throw new FatSecretException(Reason.UPSTREAM_ERROR, "FatSecret 응답을 해석할 수 없습니다", e);
        }
    }

    private FatSecretException toException(int code, String message) {
        if (code == CODE_IP_NOT_ALLOWED) {
            return new FatSecretException(Reason.IP_NOT_ALLOWED,
                    "FatSecret에 이 서버의 IP가 허용되어 있지 않습니다(FatSecret 계정에서 IP 등록 필요): " + message);
        }
        if (code == CODE_INVALID_ID) {
            return new FatSecretException(Reason.NOT_FOUND, "FatSecret에 해당 식품이 없습니다: " + message);
        }
        if (code == CODE_INVALID_VALUE) {
            return new FatSecretException(Reason.INVALID_REQUEST, "FatSecret이 요청 값을 거부했습니다: " + message);
        }
        if (code == CODE_INVALID_TOKEN) {
            return new FatSecretException(Reason.AUTH_FAILED, "FatSecret 토큰이 계속 거부됩니다: " + message);
        }
        return new FatSecretException(Reason.UPSTREAM_ERROR, "FatSecret 오류(code " + code + "): " + message);
    }

    // FatSecret JSON은 결과가 1건이면 배열이 아니라 객체 하나로, 0건이면 키 자체가 없이 내려온다.
    private static List<JsonNode> asList(JsonNode node) {
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (node.isArray()) {
            List<JsonNode> list = new ArrayList<>();
            node.forEach(list::add);
            return list;
        }
        return List.of(node);
    }

    // FatSecret은 숫자도 문자열("89")로 내려주므로 문자열로 읽고 필요한 곳에서 변환한다.
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static Double doubleValue(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static long longValue(JsonNode node, String field, long defaultValue) {
        Double value = doubleValue(node, field);
        return value == null ? defaultValue : value.longValue();
    }
}
