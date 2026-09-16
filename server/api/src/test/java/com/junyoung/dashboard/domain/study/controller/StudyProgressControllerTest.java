package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressResponse;
import com.junyoung.dashboard.domain.study.service.StudyProgressService;
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

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyProgressController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudyProgressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyProgressService studyProgressService;

    @Test
    void createsProgress() throws Exception {
        StudyProgressRequest request = new StudyProgressRequest(1L, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이");
        StudyProgressResponse response = new StudyProgressResponse(1L, 1L, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이",
                LocalDateTime.now(), LocalDateTime.now());
        when(studyProgressService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/study/progresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.minutes").value(60))
                .andExpect(jsonPath("$.data.topicId").value(1));
    }

    @Test
    void rejectsNonPositiveMinutes() throws Exception {
        StudyProgressRequest invalid = new StudyProgressRequest(1L, LocalDate.of(2026, 9, 14), 0, null);

        mockMvc.perform(post("/api/study/progresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsProgressesByTopic() throws Exception {
        when(studyProgressService.findByTopicId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new StudyProgressResponse(1L, 1L, LocalDate.of(2026, 9, 14), 60, null,
                        LocalDateTime.now(), LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/study/progresses").param("topicId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].minutes").value(60))
                .andExpect(jsonPath("$.data.content[0].topicId").value(1));
    }
}
