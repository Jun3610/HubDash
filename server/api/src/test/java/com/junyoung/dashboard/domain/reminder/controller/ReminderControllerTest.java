package com.junyoung.dashboard.domain.reminder.controller;

import com.junyoung.dashboard.domain.reminder.dto.ReminderRequest;
import com.junyoung.dashboard.domain.reminder.dto.ReminderResponse;
import com.junyoung.dashboard.domain.reminder.service.ReminderService;
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

@WebMvcTest(ReminderController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReminderService reminderService;

    @Test
    void createsReminder() throws Exception {
        ReminderRequest request = new ReminderRequest(
                "과제 마감 임박", LocalDateTime.of(2026, 9, 20, 9, 0), "pknu", 42L, false);
        ReminderResponse response = new ReminderResponse(1L, "과제 마감 임박",
                LocalDateTime.of(2026, 9, 20, 9, 0), "pknu", 42L, false,
                LocalDateTime.now(), LocalDateTime.now());
        when(reminderService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/reminder/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("과제 마감 임박"));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        ReminderRequest invalid = new ReminderRequest(
                "", LocalDateTime.of(2026, 9, 20, 9, 0), null, null, false);

        mockMvc.perform(post("/api/reminder/reminders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsReminders() throws Exception {
        when(reminderService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new ReminderResponse(1L, "과제 마감 임박", LocalDateTime.of(2026, 9, 20, 9, 0),
                        "pknu", 42L, false, LocalDateTime.now(), LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/reminder/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("과제 마감 임박"));
    }
}
