package com.junyoung.dashboard.domain.memo.controller;

import com.junyoung.dashboard.domain.memo.dto.MemoRequest;
import com.junyoung.dashboard.domain.memo.dto.MemoResponse;
import com.junyoung.dashboard.domain.memo.service.MemoService;
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

@WebMvcTest(MemoController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemoService memoService;

    @Test
    void createsMemo() throws Exception {
        MemoRequest request = new MemoRequest("장보기", "우유, 계란, 빵", "일상,장보기");
        MemoResponse response = new MemoResponse(1L, "장보기", "우유, 계란, 빵", "일상,장보기",
                LocalDateTime.now(), LocalDateTime.now());
        when(memoService.create(request)).thenReturn(response);

        mockMvc.perform(post("/api/memo/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("장보기"));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        MemoRequest invalid = new MemoRequest("", "본문", null);

        mockMvc.perform(post("/api/memo/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void rejectsBlankContent() throws Exception {
        MemoRequest invalid = new MemoRequest("제목", "", null);

        mockMvc.perform(post("/api/memo/memos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listsMemos() throws Exception {
        when(memoService.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(
                new MemoResponse(1L, "장보기", "우유, 계란, 빵", "일상,장보기", LocalDateTime.now(), LocalDateTime.now())
        )));

        mockMvc.perform(get("/api/memo/memos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("장보기"));
    }
}
