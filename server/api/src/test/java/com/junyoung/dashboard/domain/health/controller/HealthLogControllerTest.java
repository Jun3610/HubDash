package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.HealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.HealthLogResponse;
import com.junyoung.dashboard.domain.health.service.HealthLogService;
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

@WebMvcTest(HealthLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HealthLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HealthLogService healthLogService;

    @Test
    void createsHealthLog() throws Exception {
        HealthLogRequest request = new HealthLogRequest(LocalDate.of(2026, 9, 1), 70.5, 7.5, null);
        HealthLogResponse response = new HealthLogResponse(1L, LocalDate.of(2026, 9, 1), 70.5, 7.5, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(healthLogService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/health/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.weightKg").value(70.5));
    }

    @Test
    void rejectsSleepHoursOutOfRange() throws Exception {
        HealthLogRequest invalid = new HealthLogRequest(LocalDate.of(2026, 9, 1), 70.5, 25.0, null);

        mockMvc.perform(post("/api/health/logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsHealthLogs() throws Exception {
        when(healthLogService.findAll()).thenReturn(List.of(
                new HealthLogResponse(1L, LocalDate.of(2026, 9, 1), 70.5, 7.5, null,
                        LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/health/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].weightKg").value(70.5));
    }
}
