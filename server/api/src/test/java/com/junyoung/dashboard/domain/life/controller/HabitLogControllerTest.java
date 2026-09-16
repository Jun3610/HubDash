package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogResponse;
import com.junyoung.dashboard.domain.life.service.HabitLogService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HabitLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HabitLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HabitLogService habitLogService;

    @Test
    void createsLog() throws Exception {
        HabitLogRequest request = new HabitLogRequest(1L, LocalDate.of(2026, 9, 16), true, "완료");
        HabitLogResponse response = new HabitLogResponse(1L, 1L, LocalDate.of(2026, 9, 16), true, "완료",
                LocalDateTime.now(), LocalDateTime.now());
        when(habitLogService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/life/habit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.habitId").value(1));
    }

    @Test
    void listsLogsByHabit() throws Exception {
        when(habitLogService.findByHabitId(1L)).thenReturn(List.of(
                new HabitLogResponse(1L, 1L, LocalDate.of(2026, 9, 16), true, null,
                        LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/life/habit-logs").param("habitId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].habitId").value(1));
    }
}
