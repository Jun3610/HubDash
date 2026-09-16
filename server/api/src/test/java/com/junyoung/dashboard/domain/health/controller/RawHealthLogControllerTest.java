package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.RawHealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.RawHealthLogResponse;
import com.junyoung.dashboard.domain.health.service.RawHealthLogService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RawHealthLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RawHealthLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RawHealthLogService rawHealthLogService;

    @Test
    void createsRawHealthLog() throws Exception {
        RawHealthLogRequest request = new RawHealthLogRequest("2026-09-17", "70.5", 7.5, null);
        RawHealthLogResponse response = new RawHealthLogResponse(1L, "2026-09-17", "70.5", 7.5, null,
                "PENDING", null, LocalDateTime.now(), LocalDateTime.now());
        when(rawHealthLogService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/health/raw/health-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void rejectsBlankRecordedAtRaw() throws Exception {
        RawHealthLogRequest invalid = new RawHealthLogRequest("", "70.5", 7.5, null);

        mockMvc.perform(post("/api/health/raw/health-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void acceptsUnparseableWeightRawWithoutValidationError() throws Exception {
        // raw 입력은 형식 검증을 하지 않는다 — 실제 파싱/검증은 Kafka Consumer 몫.
        RawHealthLogRequest request = new RawHealthLogRequest("2026-09-17", "무거움", null, null);
        RawHealthLogResponse response = new RawHealthLogResponse(1L, "2026-09-17", "무거움", null, null,
                "PENDING", null, LocalDateTime.now(), LocalDateTime.now());
        when(rawHealthLogService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/health/raw/health-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void findsById() throws Exception {
        RawHealthLogResponse response = new RawHealthLogResponse(1L, "2026-09-17", "70.5", 7.5, null,
                "PROCESSED", null, LocalDateTime.now(), LocalDateTime.now());
        when(rawHealthLogService.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/health/raw/health-logs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PROCESSED"));
    }
}
