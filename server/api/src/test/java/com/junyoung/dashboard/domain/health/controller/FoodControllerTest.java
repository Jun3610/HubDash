package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.FoodRequest;
import com.junyoung.dashboard.domain.health.dto.FoodResponse;
import com.junyoung.dashboard.domain.health.service.FoodService;
import com.junyoung.dashboard.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FoodController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FoodService foodService;

    private FoodResponse sample() {
        return new FoodResponse(1L, "그릭요거트", 180, 12.0, 5.0, 17.0, "1컵", true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createsFood() throws Exception {
        FoodRequest request = new FoodRequest("그릭요거트", 180, 12.0, 5.0, 17.0, "1컵", true);
        when(foodService.create(request)).thenReturn(sample());

        mockMvc.perform(post("/api/health/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("그릭요거트"))
                .andExpect(jsonPath("$.data.pinned").value(true));
    }

    @Test
    void rejectsBlankNameAndNegativeCalories() throws Exception {
        FoodRequest invalid = new FoodRequest(" ", -1, null, null, null, null, null);

        mockMvc.perform(post("/api/health/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsNegativeMacro() throws Exception {
        FoodRequest invalid = new FoodRequest("바나나", 100, -3.0, null, null, null, false);

        mockMvc.perform(post("/api/health/foods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsFoods() throws Exception {
        when(foodService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(sample())));

        mockMvc.perform(get("/api/health/foods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].serving").value("1컵"));
    }
}
