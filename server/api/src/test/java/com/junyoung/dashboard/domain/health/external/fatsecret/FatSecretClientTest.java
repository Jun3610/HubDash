package com.junyoung.dashboard.domain.health.external.fatsecret;

import com.junyoung.dashboard.domain.health.dto.FoodDetailResponse;
import com.junyoung.dashboard.domain.health.dto.FoodSearchResponse;
import com.junyoung.dashboard.domain.health.external.fatsecret.FatSecretException.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

class FatSecretClientTest {

    private static final String API_URL = "https://api.test/rest/server.api";

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private MockRestServiceServer server;
    private FatSecretTokenProvider tokenProvider;
    private FatSecretClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        tokenProvider = mock(FatSecretTokenProvider.class);
        when(tokenProvider.getToken()).thenReturn("tok-1");
        client = new FatSecretClient(new FatSecretProperties("id", "secret", "https://oauth.test/token", API_URL),
                tokenProvider, builder.build(), objectMapper);
    }

    @Test
    void searchMapsMultipleResults() {
        server.expect(requestTo(startsWith(API_URL)))
                .andExpect(queryParam("method", "foods.search"))
                .andExpect(queryParam("search_expression", "banana"))
                .andExpect(queryParam("page_number", "1"))
                .andExpect(queryParam("max_results", "2"))
                .andExpect(queryParam("format", "json"))
                .andExpect(header("Authorization", "Bearer tok-1"))
                .andRespond(withSuccess("""
                        {"foods":{"food":[
                          {"food_id":"5388","food_name":"Banana","food_type":"Generic",
                           "food_description":"Per 100g - Calories: 89kcal | Fat: 0.33g | Carbs: 22.84g | Protein: 1.09g"},
                          {"food_id":"9001","food_name":"Banana Chips","brand_name":"Acme","food_type":"Brand",
                           "food_description":"Per 28g - Calories: 150kcal"}],
                         "max_results":"2","page_number":"1","total_results":"57"}}
                        """, MediaType.APPLICATION_JSON));

        FoodSearchResponse result = client.searchFoods("banana", 1, 2);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).foodId()).isEqualTo("5388");
        assertThat(result.items().get(0).name()).isEqualTo("Banana");
        assertThat(result.items().get(0).brandName()).isNull();
        assertThat(result.items().get(1).brandName()).isEqualTo("Acme");
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalResults()).isEqualTo(57);
        server.verify();
    }

    @Test
    void searchQueryWithSpecialCharactersIsEncodedNotInterpretedAsParameters() {
        server.expect(queryParam("search_expression", "mac%20%26%20cheese%3D1"))
                .andRespond(withSuccess("{\"foods\":{\"total_results\":\"0\"}}", MediaType.APPLICATION_JSON));

        assertThat(client.searchFoods("mac & cheese=1", 0, 10).items()).isEmpty();
        server.verify();
    }

    @Test
    void searchWithSingleResultReturnedAsObjectInsteadOfArray() {
        server.expect(requestTo(startsWith(API_URL)))
                .andRespond(withSuccess("""
                        {"foods":{"food":{"food_id":"1","food_name":"Only One","food_type":"Generic"},
                         "max_results":"10","page_number":"0","total_results":"1"}}
                        """, MediaType.APPLICATION_JSON));

        FoodSearchResponse result = client.searchFoods("only", 0, 10);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).name()).isEqualTo("Only One");
    }

    @Test
    void searchWithNoResultsReturnsEmptyList() {
        server.expect(requestTo(startsWith(API_URL)))
                .andRespond(withSuccess("""
                        {"foods":{"max_results":"10","total_results":"0","page_number":"0"}}
                        """, MediaType.APPLICATION_JSON));

        FoodSearchResponse result = client.searchFoods("zzz", 0, 10);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalResults()).isZero();
    }

    @Test
    void getFoodMapsServingsToMealRecordShapedNutrition() {
        server.expect(requestTo(startsWith(API_URL)))
                .andExpect(queryParam("method", "food.get.v4"))
                .andExpect(queryParam("food_id", "5388"))
                .andRespond(withSuccess("""
                        {"food":{"food_id":"5388","food_name":"Banana","food_type":"Generic",
                         "servings":{"serving":[
                           {"serving_id":"1","serving_description":"1 medium","metric_serving_amount":"118.000",
                            "metric_serving_unit":"g","calories":"105","carbohydrate":"26.95","protein":"1.29",
                            "fat":"0.39","sodium":"1"},
                           {"serving_id":"2","serving_description":"100 g","calories":"89.4","carbohydrate":"22.84"}
                         ]}}}
                        """, MediaType.APPLICATION_JSON));

        FoodDetailResponse food = client.getFood("5388");

        assertThat(food.foodId()).isEqualTo("5388");
        assertThat(food.name()).isEqualTo("Banana");
        assertThat(food.servings()).hasSize(2);
        var first = food.servings().get(0);
        assertThat(first.description()).isEqualTo("1 medium");
        assertThat(first.metricServingAmount()).isEqualTo(118.0);
        assertThat(first.metricServingUnit()).isEqualTo("g");
        assertThat(first.calories()).isEqualTo(105);
        assertThat(first.carbsG()).isEqualTo(26.95);
        assertThat(first.proteinG()).isEqualTo(1.29);
        assertThat(first.fatG()).isEqualTo(0.39);
        assertThat(first.sodiumMg()).isEqualTo(1.0);
        var second = food.servings().get(1);
        assertThat(second.calories()).isEqualTo(89); // 반올림
        assertThat(second.proteinG()).isNull(); // 응답에 없는 영양소는 null
        assertThat(second.sodiumMg()).isNull();
    }

    @Test
    void getFoodWithSingleServingObject() {
        server.expect(requestTo(startsWith(API_URL)))
                .andRespond(withSuccess("""
                        {"food":{"food_id":"7","food_name":"Rice","servings":{"serving":
                          {"serving_id":"1","serving_description":"1 cup","calories":"200"}}}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.getFood("7").servings()).hasSize(1);
    }

    @Test
    void getFoodWithoutFoodNodeIsUpstreamError() {
        server.expect(requestTo(startsWith(API_URL)))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getFood("1"))
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UPSTREAM_ERROR));
    }

    // 실제 호출로 확인된 응답: HTTP 200 + {"error":{"code":21,"message":"Invalid IP address detected: ..."}}
    @Test
    void ipNotAllowedErrorInBodyIsMappedEvenWithHttp200() {
        server.expect(requestTo(startsWith(API_URL)))
                .andRespond(withSuccess("""
                        { "error": {"code": 21, "message": "Invalid IP address detected:  '1.2.3.4'" }}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchFoods("banana", 0, 10))
                .isInstanceOfSatisfying(FatSecretException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.IP_NOT_ALLOWED);
                    assertThat(e.getMessage()).contains("1.2.3.4");
                });
        verify(tokenProvider, never()).invalidate();
    }

    // 실제 호출로 확인된 응답: {"error":{"code":13,"message":"Invalid token: Unable to decode token"}}
    @Test
    void invalidTokenErrorInvalidatesTokenAndRetriesOnceWithNewToken() {
        when(tokenProvider.getToken()).thenReturn("stale", "fresh");
        server.expect(once(), requestTo(startsWith(API_URL)))
                .andExpect(header("Authorization", "Bearer stale"))
                .andRespond(withSuccess("""
                        {"error":{"code":13,"message":"Invalid token: Unable to decode token"}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(once(), requestTo(startsWith(API_URL)))
                .andExpect(header("Authorization", "Bearer fresh"))
                .andRespond(withSuccess("{\"foods\":{\"total_results\":\"0\"}}", MediaType.APPLICATION_JSON));

        assertThat(client.searchFoods("banana", 0, 10).items()).isEmpty();

        verify(tokenProvider, times(1)).invalidate();
        server.verify();
    }

    @Test
    void invalidTokenPersistingAfterRetryBecomesAuthFailedWithoutLooping() {
        server.expect(org.springframework.test.web.client.ExpectedCount.times(2), requestTo(startsWith(API_URL)))
                .andRespond(withSuccess("""
                        {"error":{"code":13,"message":"Invalid token"}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchFoods("banana", 0, 10))
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.AUTH_FAILED));
        server.verify();
    }

    @Test
    void unknownErrorCodeIsUpstreamErrorAndKeepsCodeInMessage() {
        server.expect(requestTo(startsWith(API_URL)))
                .andRespond(withSuccess("""
                        {"error":{"code":999,"message":"Something odd"}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getFood("1"))
                .isInstanceOfSatisfying(FatSecretException.class, e -> {
                    assertThat(e.getReason()).isEqualTo(Reason.UPSTREAM_ERROR);
                    assertThat(e.getMessage()).contains("999").contains("Something odd");
                });
    }

    @Test
    void http429IsRateLimited() {
        server.expect(requestTo(startsWith(API_URL))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.searchFoods("banana", 0, 10))
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.RATE_LIMITED));
    }

    @Test
    void http401InvalidatesTokenAndIsAuthFailed() {
        server.expect(requestTo(startsWith(API_URL))).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.searchFoods("banana", 0, 10))
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.AUTH_FAILED));
        verify(tokenProvider).invalidate();
    }

    @Test
    void http500IsUpstreamError() {
        server.expect(requestTo(startsWith(API_URL))).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.searchFoods("banana", 0, 10))
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UPSTREAM_ERROR));
    }

    @Test
    void networkFailureIsUpstreamError() {
        server.expect(requestTo(startsWith(API_URL))).andRespond(withException(new IOException("timeout")));

        assertThatThrownBy(() -> client.searchFoods("banana", 0, 10))
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UPSTREAM_ERROR));
    }

    @Test
    void nonJsonBodyIsUpstreamError() {
        server.expect(requestTo(startsWith(API_URL)))
                .andRespond(withSuccess("<html>gateway</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.searchFoods("banana", 0, 10))
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UPSTREAM_ERROR));
    }
}
