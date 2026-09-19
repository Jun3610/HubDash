package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.FoodDetailResponse;
import com.junyoung.dashboard.domain.health.dto.FoodSearchItemResponse;
import com.junyoung.dashboard.domain.health.dto.FoodSearchResponse;
import com.junyoung.dashboard.domain.health.dto.FoodServingResponse;
import com.junyoung.dashboard.domain.health.external.fatsecret.FatSecretClient;
import com.junyoung.dashboard.domain.health.external.fatsecret.FatSecretException;
import com.junyoung.dashboard.domain.health.external.fatsecret.FatSecretException.Reason;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FatSecretClient fatSecretClient;

    @Test
    void searchesFoodsWithDefaultPaging() throws Exception {
        when(fatSecretClient.searchFoods("banana", 0, 10)).thenReturn(new FoodSearchResponse(
                List.of(new FoodSearchItemResponse("5388", "Banana", null, "Generic", "Per 100g")), 0, 10, 1));

        mockMvc.perform(get("/api/health/foods/search").param("query", "banana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].foodId").value("5388"))
                .andExpect(jsonPath("$.data.totalResults").value(1));
    }

    @Test
    void getsFoodDetailWithNutritionShapedLikeMealRecord() throws Exception {
        when(fatSecretClient.getFood("5388")).thenReturn(new FoodDetailResponse("5388", "Banana", null,
                List.of(new FoodServingResponse("1", "1 medium", 118.0, "g", 105, 26.95, 1.29, 0.39, 1.0))));

        mockMvc.perform(get("/api/health/foods/5388"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.servings[0].calories").value(105))
                .andExpect(jsonPath("$.data.servings[0].carbsG").value(26.95))
                .andExpect(jsonPath("$.data.servings[0].sodiumMg").value(1.0));
    }

    @Test
    void blankQueryIsBadRequestAndDoesNotCallUpstream() throws Exception {
        mockMvc.perform(get("/api/health/foods/search").param("query", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        verifyNoInteractions(fatSecretClient);
    }

    @Test
    void missingQueryIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/health/foods/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedPageSizeIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/health/foods/search").param("query", "banana").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        verify(fatSecretClient, never()).searchFoods(anyString(), anyInt(), anyInt());
    }

    @Test
    void negativePageIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/health/foods/search").param("query", "banana").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void notConfiguredIs503() throws Exception {
        when(fatSecretClient.searchFoods("banana", 0, 10))
                .thenThrow(new FatSecretException(Reason.NOT_CONFIGURED, "키 없음"));

        mockMvc.perform(get("/api/health/foods/search").param("query", "banana"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("FATSECRET_NOT_CONFIGURED"));
    }

    @Test
    void ipNotAllowedIs502WithActionableMessage() throws Exception {
        when(fatSecretClient.getFood("1"))
                .thenThrow(new FatSecretException(Reason.IP_NOT_ALLOWED, "IP 등록 필요: 1.2.3.4"));

        mockMvc.perform(get("/api/health/foods/1"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("FATSECRET_IP_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("IP 등록 필요: 1.2.3.4"));
    }

    @Test
    void rateLimitedIs429() throws Exception {
        when(fatSecretClient.searchFoods("banana", 0, 10))
                .thenThrow(new FatSecretException(Reason.RATE_LIMITED, "한도 초과"));

        mockMvc.perform(get("/api/health/foods/search").param("query", "banana"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.errorCode").value("FATSECRET_RATE_LIMITED"));
    }

    @Test
    void upstreamAndAuthFailuresAre502() throws Exception {
        when(fatSecretClient.searchFoods("a", 0, 10))
                .thenThrow(new FatSecretException(Reason.UPSTREAM_ERROR, "boom"));
        when(fatSecretClient.searchFoods("b", 0, 10))
                .thenThrow(new FatSecretException(Reason.AUTH_FAILED, "bad creds"));

        mockMvc.perform(get("/api/health/foods/search").param("query", "a"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("FATSECRET_UPSTREAM_ERROR"));
        mockMvc.perform(get("/api/health/foods/search").param("query", "b"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("FATSECRET_AUTH_FAILED"));
    }

    @Test
    void unknownFoodIs404() throws Exception {
        when(fatSecretClient.getFood("1"))
                .thenThrow(new FatSecretException(Reason.NOT_FOUND, "식품 없음"));

        mockMvc.perform(get("/api/health/foods/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FATSECRET_NOT_FOUND"));
    }

    @Test
    void nonNumericFoodIdIs400() throws Exception {
        when(fatSecretClient.getFood("abc"))
                .thenThrow(new FatSecretException(Reason.INVALID_REQUEST, "값 오류"));

        mockMvc.perform(get("/api/health/foods/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("FATSECRET_INVALID_REQUEST"));
    }
}
