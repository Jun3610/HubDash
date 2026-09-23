package com.junyoung.dashboard.domain.study.controller;

import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicResponse;
import com.junyoung.dashboard.domain.study.service.StudyTopicService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StudyTopicController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class StudyTopicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StudyTopicService studyTopicService;

    @Test
    void createsTopic() throws Exception {
        StudyTopicRequest request = new StudyTopicRequest("토익", "영어 공부", null);
        StudyTopicResponse response = new StudyTopicResponse(1L, "토익", "영어 공부", null,
                LocalDateTime.now(), LocalDateTime.now());
        when(studyTopicService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/study/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("토익"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        StudyTopicRequest invalid = new StudyTopicRequest("", null, null);

        mockMvc.perform(post("/api/study/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsTopics() throws Exception {
        when(studyTopicService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new StudyTopicResponse(1L, "토익", null, null, LocalDateTime.now(), LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/study/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].name").value("토익"));
    }
}
