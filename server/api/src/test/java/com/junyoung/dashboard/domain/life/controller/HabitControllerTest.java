package com.junyoung.dashboard.domain.life.controller;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.service.HabitService;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HabitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class HabitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HabitService habitService;

    @Test
    void createsHabit() throws Exception {
        HabitRequest request = new HabitRequest("아침 스트레칭", "매일 아침 10분");
        HabitResponse response = new HabitResponse(1L, "아침 스트레칭", "매일 아침 10분",
                LocalDateTime.now(), LocalDateTime.now());
        when(habitService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/life/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("아침 스트레칭"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        HabitRequest invalid = new HabitRequest("", null);

        mockMvc.perform(post("/api/life/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsHabits() throws Exception {
        when(habitService.findAll()).thenReturn(List.of(
                new HabitResponse(1L, "아침 스트레칭", null, LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/life/habits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("아침 스트레칭"));
    }
}
