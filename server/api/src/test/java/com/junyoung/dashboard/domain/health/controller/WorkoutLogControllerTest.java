package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.WorkoutLogRequest;
import com.junyoung.dashboard.domain.health.dto.WorkoutLogResponse;
import com.junyoung.dashboard.domain.health.service.WorkoutLogService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkoutLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class WorkoutLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WorkoutLogService workoutLogService;

    @Test
    void createsWorkoutLog() throws Exception {
        WorkoutLogRequest request = new WorkoutLogRequest(LocalDate.of(2026, 9, 1), "러닝", 30, 300, null);
        WorkoutLogResponse response = new WorkoutLogResponse(1L, LocalDate.of(2026, 9, 1), "러닝", 30, 300, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(workoutLogService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/health/workout-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("러닝"));
    }

    @Test
    void rejectsZeroDuration() throws Exception {
        WorkoutLogRequest invalid = new WorkoutLogRequest(LocalDate.of(2026, 9, 1), "러닝", 0, null, null);

        mockMvc.perform(post("/api/health/workout-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsWorkoutLogs() throws Exception {
        when(workoutLogService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new WorkoutLogResponse(1L, LocalDate.of(2026, 9, 1), "러닝", 30, 300, null,
                        LocalDateTime.now(), LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/health/workout-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("러닝"));
    }
}
