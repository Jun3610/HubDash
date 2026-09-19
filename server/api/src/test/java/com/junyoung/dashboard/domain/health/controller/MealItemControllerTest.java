package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.MealItemRequest;
import com.junyoung.dashboard.domain.health.dto.MealItemResponse;
import com.junyoung.dashboard.domain.health.service.MealItemService;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealItemController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MealItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MealItemService mealItemService;

    private MealItemResponse response(long id) {
        return new MealItemResponse(id, 1L, "신라면", 520, 83.0, 11.0, 16.0, 1970.0,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createsItem() throws Exception {
        MealItemRequest request = new MealItemRequest(1L, "신라면", 520, 83.0, 11.0, 16.0, 1970.0);
        when(mealItemService.create(request)).thenReturn(response(1L));

        mockMvc.perform(post("/api/health/meal-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("신라면"))
                .andExpect(jsonPath("$.data.calories").value(520));
    }

    @Test
    void acceptsItemWithOnlyNameAndCalories() throws Exception {
        // 탄단지를 모르는 음식도 이름+칼로리만으로 등록할 수 있어야 한다.
        MealItemRequest request = new MealItemRequest(1L, "김치", 15, null, null, null, null);
        when(mealItemService.create(request)).thenReturn(response(2L));

        mockMvc.perform(post("/api/health/meal-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void rejectsInvalidRequests() throws Exception {
        MealItemRequest[] invalid = {
                new MealItemRequest(null, "밥", 300, null, null, null, null),      // 끼니 없음
                new MealItemRequest(1L, " ", 300, null, null, null, null),         // 이름 공백
                new MealItemRequest(1L, "밥", null, null, null, null, null),       // 칼로리 없음
                new MealItemRequest(1L, "밥", -1, null, null, null, null),         // 음수 칼로리
                new MealItemRequest(1L, "밥", 300, -0.1, null, null, null),        // 음수 탄수화물
                new MealItemRequest(1L, "밥", 300, null, -1.0, null, null),        // 음수 단백질
                new MealItemRequest(1L, "밥", 300, null, null, -1.0, null),        // 음수 지방
                new MealItemRequest(1L, "x".repeat(101), 300, null, null, null, null), // 이름 100자 초과
        };
        for (MealItemRequest request : invalid) {
            mockMvc.perform(post("/api/health/meal-items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
        verifyNoInteractions(mealItemService);
    }

    @Test
    void createForMissingMealRecordIs404() throws Exception {
        MealItemRequest request = new MealItemRequest(99L, "밥", 300, null, null, null, null);
        when(mealItemService.create(request)).thenThrow(new EntityNotFoundException("meal record 99 not found"));

        mockMvc.perform(post("/api/health/meal-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsItemsOfMealRecord() throws Exception {
        when(mealItemService.findByMealRecordId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response(1L))));

        mockMvc.perform(get("/api/health/meal-items").param("mealRecordId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("신라면"));
    }

    @Test
    void listWithoutMealRecordIdIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/health/meal-items"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatesItem() throws Exception {
        MealItemRequest request = new MealItemRequest(1L, "신라면 반개", 260, 41.0, 5.0, 8.0, 985.0);
        when(mealItemService.update(1L, request)).thenReturn(response(1L));

        mockMvc.perform(put("/api/health/meal-items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void deletesItem() throws Exception {
        mockMvc.perform(delete("/api/health/meal-items/1"))
                .andExpect(status().isNoContent());
        verify(mealItemService).delete(1L);
    }
}
