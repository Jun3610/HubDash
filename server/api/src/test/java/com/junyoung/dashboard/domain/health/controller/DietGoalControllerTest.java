package com.junyoung.dashboard.domain.health.controller;

import com.junyoung.dashboard.domain.health.dto.DietGoalResponse;
import com.junyoung.dashboard.domain.health.entity.GoalRule;
import com.junyoung.dashboard.domain.health.service.DietGoalService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DietGoalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class DietGoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DietGoalService dietGoalService;

    @Test
    void returnsGoalWithNullValuesWhenNotSet() throws Exception {
        when(dietGoalService.get()).thenReturn(new DietGoalResponse(1L, null, GoalRule.AT_MOST, null, GoalRule.AT_MOST,
                null, GoalRule.AT_LEAST, null, GoalRule.AT_MOST, LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(get("/api/health/diet-goal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.carbsG").doesNotExist())
                .andExpect(jsonPath("$.data.proteinRule").value("AT_LEAST"));
    }

    @Test
    void rejectsMissingRuleAndNegativeValue() throws Exception {
        String body = """
                {"carbsG": -1, "carbsRule": "AT_MOST", "fatRule": "AT_MOST", "proteinRule": "AT_LEAST"}
                """;
        mockMvc.perform(put("/api/health/diet-goal").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsUnknownRule() throws Exception {
        String body = """
                {"carbsRule": "ABOUT", "fatRule": "AT_MOST", "proteinRule": "AT_LEAST", "caloriesRule": "AT_MOST"}
                """;
        mockMvc.perform(put("/api/health/diet-goal").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }
}
