package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.MealRecordRequest;
import com.junyoung.dashboard.domain.health.dto.MealRecordResponse;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.domain.health.service.MealRecordService;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MealRecordController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MealRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MealRecordService mealRecordService;

    @Test
    void createsMealRecord() throws Exception {
        MealRecordRequest request = new MealRecordRequest(
                LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, 500, 60.0, 20.0, 15.0, 300.0, null);
        MealRecordResponse response = new MealRecordResponse(1L, LocalDateTime.of(2026, 9, 1, 8, 0),
                MealType.BREAKFAST, 500, 60.0, 20.0, 15.0, 300.0, null, LocalDateTime.now(), LocalDateTime.now());
        when(mealRecordService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/health/meal-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.mealType").value("BREAKFAST"));
    }

    @Test
    void rejectsNegativeCalories() throws Exception {
        MealRecordRequest invalid = new MealRecordRequest(
                LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, -100, null, null, null, null, null);

        mockMvc.perform(post("/api/health/meal-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsMealRecords() throws Exception {
        when(mealRecordService.findAll()).thenReturn(List.of(
                new MealRecordResponse(1L, LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, 500,
                        60.0, 20.0, 15.0, 300.0, null, LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/health/meal-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mealType").value("BREAKFAST"));
    }
}
