package com.junyoung.dashboard.domain.hub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryResponse;
import com.junyoung.dashboard.domain.hub.service.HubCategoryService;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HubCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, HubCategoryControllerTest.TestConfig.class})
class HubCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HubCategoryService hubCategoryService;

    static class TestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @Test
    void createsCategory() throws Exception {
        HubCategoryRequest request = new HubCategoryRequest("CI/CD", "빌드 파이프라인 문서");
        HubCategoryResponse response = new HubCategoryResponse(1L, "CI/CD", "빌드 파이프라인 문서",
                LocalDateTime.now(), LocalDateTime.now());
        when(hubCategoryService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/hub/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("CI/CD"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        HubCategoryRequest invalid = new HubCategoryRequest("", null);

        mockMvc.perform(post("/api/hub/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsCategories() throws Exception {
        when(hubCategoryService.findAll()).thenReturn(List.of(
                new HubCategoryResponse(1L, "CI/CD", null, LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/hub/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("CI/CD"));
    }
}
