package com.junyoung.dashboard.domain.user.controller;

import com.junyoung.dashboard.domain.user.dto.UserProfileRequest;
import com.junyoung.dashboard.domain.user.dto.UserProfileResponse;
import com.junyoung.dashboard.domain.user.service.UserProfileService;
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

@WebMvcTest(UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserProfileService userProfileService;

    @Test
    void getsProfile() throws Exception {
        UserProfileResponse response = new UserProfileResponse(1L, "사용자", null, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(userProfileService.getProfile()).thenReturn(response);

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("사용자"));
    }

    @Test
    void updatesProfile() throws Exception {
        UserProfileRequest request = new UserProfileRequest("박준영", "junp3610@example.com", "백엔드 개발자");
        UserProfileResponse response = new UserProfileResponse(1L, "박준영", "junp3610@example.com", "백엔드 개발자",
                LocalDateTime.now(), LocalDateTime.now());
        when(userProfileService.update(request)).thenReturn(response);

        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.displayName").value("박준영"));
    }

    @Test
    void rejectsBlankDisplayName() throws Exception {
        UserProfileRequest invalid = new UserProfileRequest("", null, null);

        mockMvc.perform(put("/api/user/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
