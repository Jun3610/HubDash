package com.junyoung.dashboard.domain.pknu.controller;

import com.junyoung.dashboard.domain.pknu.dto.CourseRequest;
import com.junyoung.dashboard.domain.pknu.dto.CourseResponse;
import com.junyoung.dashboard.domain.pknu.service.CourseService;
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

@WebMvcTest(CourseController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CourseService courseService;

    @Test
    void createsCourse() throws Exception {
        CourseRequest request = new CourseRequest(1L, "자료구조", "김교수", 3);
        CourseResponse response = new CourseResponse(1L, 1L, "자료구조", "김교수", 3, LocalDateTime.now(), LocalDateTime.now());
        when(courseService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/pknu/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("자료구조"));
    }

    @Test
    void rejectsCreditOutOfRange() throws Exception {
        CourseRequest invalid = new CourseRequest(1L, "자료구조", "김교수", 7);

        mockMvc.perform(post("/api/pknu/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsCoursesBySemesterId() throws Exception {
        when(courseService.findBySemesterId(1L)).thenReturn(List.of(
                new CourseResponse(1L, 1L, "자료구조", "김교수", 3, LocalDateTime.now(), LocalDateTime.now())
        ));

        mockMvc.perform(get("/api/pknu/courses").param("semesterId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("자료구조"));
    }
}
