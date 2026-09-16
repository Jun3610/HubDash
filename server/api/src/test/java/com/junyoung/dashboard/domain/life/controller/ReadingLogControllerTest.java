package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogResponse;
import com.junyoung.dashboard.domain.life.service.ReadingLogService;
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

@WebMvcTest(ReadingLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReadingLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReadingLogService readingLogService;

    @Test
    void createsReadingLog() throws Exception {
        ReadingLogRequest request = new ReadingLogRequest("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, 5, null);
        ReadingLogResponse response = new ReadingLogResponse(1L, "클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, 5, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(readingLogService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/life/reading-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("클린 코드"));
    }

    @Test
    void rejectsRatingOutOfRange() throws Exception {
        ReadingLogRequest invalid = new ReadingLogRequest("클린 코드", null, LocalDate.of(2026, 9, 1), null, 6, null);

        mockMvc.perform(post("/api/life/reading-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsReadingLogs() throws Exception {
        when(readingLogService.findAll()).thenReturn(List.of(
                new ReadingLogResponse(1L, "클린 코드", null, LocalDate.of(2026, 9, 1), null, null, null,
                        LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/life/reading-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("클린 코드"));
    }
}
