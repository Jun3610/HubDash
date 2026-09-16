package com.junyoung.dashboard.domain.pknu.controller;

import com.junyoung.dashboard.domain.pknu.dto.SemesterRequest;
import com.junyoung.dashboard.domain.pknu.dto.SemesterResponse;
import com.junyoung.dashboard.domain.pknu.service.SemesterService;
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

@WebMvcTest(SemesterController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class SemesterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SemesterService semesterService;

    @Test
    void createsSemester() throws Exception {
        SemesterRequest request = new SemesterRequest("2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));
        SemesterResponse response = new SemesterResponse(1L, "2026-1학기", LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 6, 30), LocalDateTime.now(), LocalDateTime.now());
        when(semesterService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/pknu/semesters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("2026-1학기"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        SemesterRequest invalid = new SemesterRequest("", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30));

        mockMvc.perform(post("/api/pknu/semesters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsSemesters() throws Exception {
        when(semesterService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new SemesterResponse(1L, "2026-1학기", LocalDate.of(2026, 3, 1), LocalDate.of(2026, 6, 30),
                        LocalDateTime.now(), LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/pknu/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("2026-1학기"));
    }
}
