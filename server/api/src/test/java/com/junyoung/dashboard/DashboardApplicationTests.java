package com.junyoung.dashboard;

import tools.jackson.databind.ObjectMapper;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.study.dto.StudyProgressRequest;
import com.junyoung.dashboard.domain.study.dto.StudyTopicRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void createsAndFetchesHubCategoryEndToEnd() throws Exception {
        HubCategoryRequest request = new HubCategoryRequest("CI/CD", "빌드 파이프라인 문서");

        mockMvc.perform(post("/api/hub/categories")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/hub/categories")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("CI/CD"));
    }

    @Test
    void createsAndFetchesStudyTopicEndToEnd() throws Exception {
        StudyTopicRequest request = new StudyTopicRequest("토익", "영어 공부");

        mockMvc.perform(post("/api/study/topics")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").exists());

        mockMvc.perform(get("/api/study/topics")
                        .header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("토익"));
    }

    @Test
    void movesStudyProgressBetweenTopicsEndToEnd() throws Exception {
        StudyTopicRequest topicARequest = new StudyTopicRequest("토익", "영어 공부");
        String topicAResponse = mockMvc.perform(post("/api/study/topics")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topicARequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long topicAId = objectMapper.readTree(topicAResponse).path("data").path("id").asLong();

        StudyTopicRequest topicBRequest = new StudyTopicRequest("알고리즘", "코딩테스트 공부");
        String topicBResponse = mockMvc.perform(post("/api/study/topics")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(topicBRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long topicBId = objectMapper.readTree(topicBResponse).path("data").path("id").asLong();

        StudyProgressRequest createRequest = new StudyProgressRequest(topicAId, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이");
        String progressResponse = mockMvc.perform(post("/api/study/progresses")
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.topicId").value(topicAId))
                .andReturn().getResponse().getContentAsString();
        long progressId = objectMapper.readTree(progressResponse).path("data").path("id").asLong();

        StudyProgressRequest moveRequest = new StudyProgressRequest(topicBId, LocalDate.of(2026, 9, 15), 90, "DP 복습");
        mockMvc.perform(put("/api/study/progresses/" + progressId)
                        .header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topicId").value(topicBId));
    }
}
