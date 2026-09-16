package com.junyoung.dashboard.domain.schedule.controller;

import com.junyoung.dashboard.domain.schedule.dto.EventRequest;
import com.junyoung.dashboard.domain.schedule.dto.EventResponse;
import com.junyoung.dashboard.domain.schedule.service.EventService;
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

@WebMvcTest(EventController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @Test
    void createsEvent() throws Exception {
        EventRequest request = new EventRequest("발표 준비", LocalDateTime.of(2026, 9, 16, 10, 0),
                LocalDateTime.of(2026, 9, 16, 11, 0), "회의실 A", null, false);
        EventResponse response = new EventResponse(1L, "발표 준비", LocalDateTime.of(2026, 9, 16, 10, 0),
                LocalDateTime.of(2026, 9, 16, 11, 0), "회의실 A", null, false,
                LocalDateTime.now(), LocalDateTime.now());
        when(eventService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/schedule/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("발표 준비"));
    }

    @Test
    void rejectsEndAtBeforeStartAt() throws Exception {
        EventRequest invalid = new EventRequest("잘못된 일정", LocalDateTime.of(2026, 9, 16, 11, 0),
                LocalDateTime.of(2026, 9, 16, 10, 0), null, null, false);

        mockMvc.perform(post("/api/schedule/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        EventRequest invalid = new EventRequest("", LocalDateTime.of(2026, 9, 16, 10, 0),
                LocalDateTime.of(2026, 9, 16, 11, 0), null, null, false);

        mockMvc.perform(post("/api/schedule/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsEvents() throws Exception {
        when(eventService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new EventResponse(1L, "발표 준비", LocalDateTime.of(2026, 9, 16, 10, 0),
                        LocalDateTime.of(2026, 9, 16, 11, 0), "회의실 A", null, false,
                        LocalDateTime.now(), LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/schedule/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("발표 준비"));
    }
}
