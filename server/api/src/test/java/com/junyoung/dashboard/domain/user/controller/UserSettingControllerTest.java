package com.junyoung.dashboard.domain.user.controller;

import com.junyoung.dashboard.domain.user.dto.UserSettingRequest;
import com.junyoung.dashboard.domain.user.dto.UserSettingResponse;
import com.junyoung.dashboard.domain.user.service.UserSettingService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserSettingController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserSettingService userSettingService;

    @Test
    void getsSettings() throws Exception {
        UserSettingResponse response = new UserSettingResponse(1L, "LIGHT", "ko", true,
                LocalDateTime.now(), LocalDateTime.now());
        when(userSettingService.getSettings()).thenReturn(response);

        mockMvc.perform(get("/api/user/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("LIGHT"));
    }

    @Test
    void updatesSettings() throws Exception {
        UserSettingRequest request = new UserSettingRequest("DARK", "en", false);
        UserSettingResponse response = new UserSettingResponse(1L, "DARK", "en", false,
                LocalDateTime.now(), LocalDateTime.now());
        when(userSettingService.update(request)).thenReturn(response);

        mockMvc.perform(put("/api/user/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.theme").value("DARK"));
    }

    @Test
    void rejectsBlankTheme() throws Exception {
        UserSettingRequest invalid = new UserSettingRequest("", "ko", true);

        mockMvc.perform(put("/api/user/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsBlankLanguage() throws Exception {
        UserSettingRequest invalid = new UserSettingRequest("LIGHT", "", true);

        mockMvc.perform(put("/api/user/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
