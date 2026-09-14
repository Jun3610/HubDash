package com.junyoung.dashboard.domain.hub.controller;

import tools.jackson.databind.ObjectMapper;
import com.junyoung.dashboard.domain.hub.dto.HubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.HubLinkResponse;
import com.junyoung.dashboard.domain.hub.service.HubLinkService;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

@WebMvcTest(HubLinkController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HubLinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HubLinkService hubLinkService;

    @Test
    void createsLink() throws Exception {
        HubLinkRequest request = new HubLinkRequest(1L, "Docker 문서", "https://example.com/docker", null);
        HubLinkResponse response = new HubLinkResponse(1L, 1L, "Docker 문서", "https://example.com/docker", null,
                LocalDateTime.now(), LocalDateTime.now());
        when(hubLinkService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/hub/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Docker 문서"));
    }

    @Test
    void listsLinksByCategory() throws Exception {
        when(hubLinkService.findByCategoryId(1L)).thenReturn(List.of(
                new HubLinkResponse(1L, 1L, "Docker 문서", "https://example.com/docker", null,
                        LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/hub/links").param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Docker 문서"));
    }
}
