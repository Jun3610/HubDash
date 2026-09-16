package com.junyoung.dashboard.domain.pknu.controller;

import com.junyoung.dashboard.domain.pknu.dto.AssignmentRequest;
import com.junyoung.dashboard.domain.pknu.dto.AssignmentResponse;
import com.junyoung.dashboard.domain.pknu.service.AssignmentService;
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

@WebMvcTest(AssignmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssignmentService assignmentService;

    @Test
    void createsAssignment() throws Exception {
        AssignmentRequest request = new AssignmentRequest(1L, "1주차 과제", LocalDate.of(2026, 3, 10), false, null);
        AssignmentResponse response = new AssignmentResponse(1L, 1L, "1주차 과제", LocalDate.of(2026, 3, 10), false,
                null, LocalDateTime.now(), LocalDateTime.now());
        when(assignmentService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/pknu/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("1주차 과제"));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        AssignmentRequest invalid = new AssignmentRequest(1L, "", LocalDate.of(2026, 3, 10), false, null);

        mockMvc.perform(post("/api/pknu/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsAssignmentsByCourseId() throws Exception {
        when(assignmentService.findByCourseId(eq(1L), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new AssignmentResponse(1L, 1L, "1주차 과제", LocalDate.of(2026, 3, 10), false, null,
                        LocalDateTime.now(), LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/pknu/assignments").param("courseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("1주차 과제"));
    }
}
