package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.DailyMealSummaryResponse;
import com.junyoung.dashboard.domain.health.dto.DailyMealSummaryResponse.MealTypeSummary;
import com.junyoung.dashboard.domain.health.dto.MealRecordRequest;
import com.junyoung.dashboard.domain.health.dto.MealRecordResponse;
import com.junyoung.dashboard.domain.health.dto.MealTotals;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.domain.health.service.MealRecordService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MealRecordControllerTest {

    private static final MealTotals ZERO = new MealTotals(0, 0.0, 0.0, 0.0, 0.0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MealRecordService mealRecordService;

    private MealRecordResponse response(long id, MealType type) {
        return new MealRecordResponse(id, LocalDateTime.of(2026, 9, 1, 8, 0), type, null,
                List.of(), ZERO, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createsMealRecord() throws Exception {
        MealRecordRequest request = new MealRecordRequest(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, null);
        when(mealRecordService.create(request)).thenReturn(response(1L, MealType.BREAKFAST));

        mockMvc.perform(post("/api/health/meal-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$.data.totals.calories").value(0));
    }

    @Test
    void rejectsMissingMealType() throws Exception {
        MealRecordRequest invalid = new MealRecordRequest(LocalDateTime.of(2026, 9, 1, 8, 0), null, null);

        mockMvc.perform(post("/api/health/meal-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsMealRecords() throws Exception {
        when(mealRecordService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response(1L, MealType.BREAKFAST))));

        mockMvc.perform(get("/api/health/meal-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].mealType").value("BREAKFAST"));
    }

    @Test
    void returnsDailySummary() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 19);
        when(mealRecordService.dailySummary(date)).thenReturn(new DailyMealSummaryResponse(date,
                new MealTotals(1200, 150.0, 60.0, 30.0, 2000.0),
                List.of(new MealTypeSummary(MealType.BREAKFAST, 2, new MealTotals(450, 67.0, 17.0, 11.0, 142.0)),
                        new MealTypeSummary(MealType.LUNCH, 0, ZERO),
                        new MealTypeSummary(MealType.DINNER, 0, ZERO),
                        new MealTypeSummary(MealType.SNACK, 0, ZERO))));

        mockMvc.perform(get("/api/health/meal-records/daily-summary").param("date", "2026-09-19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.date").value("2026-09-19"))
                .andExpect(jsonPath("$.data.totals.calories").value(1200))
                .andExpect(jsonPath("$.data.meals.length()").value(4))
                .andExpect(jsonPath("$.data.meals[0].mealType").value("BREAKFAST"))
                .andExpect(jsonPath("$.data.meals[0].itemCount").value(2))
                .andExpect(jsonPath("$.data.meals[0].totals.proteinG").value(17.0));
    }

    @Test
    void dailySummaryIsNotShadowedByFindByIdRoute() throws Exception {
        // "/daily-summary"가 "/{id}"(Long)로 잘못 매칭되면 타입 오류 400이 났을 것 — 정상 200이어야 한다.
        LocalDate date = LocalDate.of(2026, 9, 19);
        when(mealRecordService.dailySummary(date)).thenReturn(new DailyMealSummaryResponse(date, ZERO, List.of()));

        mockMvc.perform(get("/api/health/meal-records/daily-summary").param("date", "2026-09-19"))
                .andExpect(status().isOk());
    }

    @Test
    void dailySummaryWithoutDateIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/health/meal-records/daily-summary"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
        verifyNoInteractions(mealRecordService);
    }

    @Test
    void dailySummaryWithMalformedDateIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/health/meal-records/daily-summary").param("date", "2026-13-45"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/health/meal-records/daily-summary").param("date", "yesterday"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(mealRecordService);
    }
}
