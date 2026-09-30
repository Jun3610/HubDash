package com.junyoung.dashboard.domain.integration;

import com.junyoung.dashboard.domain.integration.repository.IntegrationCredentialRepository;
import com.junyoung.dashboard.global.notion.NotionClient;
import com.junyoung.dashboard.global.notion.NotionTokenSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CREDENTIAL_KEY가 없으면: 저장은 외부 확인 전에 바로 명확한 에러, 기존 환경변수 NOTION_TOKEN은 계속 쓰인다 (이슈 #227)
@SpringBootTest(properties = {"app.credential-key=", "app.notion.token=ntn_fromEnvfromEnvfromEnvABC"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrationWithoutKeyTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private IntegrationCredentialRepository repository;
    @Autowired
    private NotionTokenSource tokens;
    @MockitoBean
    private NotionClient notion;

    @Test
    void saveFailsWithClearErrorBeforeCallingNotion() throws Exception {
        mvc.perform(put("/api/integrations/notion").header("X-API-KEY", "test-api-key")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"ntn_newnewnewnewnew\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CREDENTIAL_KEY_MISSING"));
        verify(notion, never()).me(any());
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void envTokenStillWorksAndIsOnlyShownMasked() throws Exception {
        assertThat(tokens.currentToken()).isEqualTo("ntn_fromEnvfromEnvfromEnvABC");
        String body = mvc.perform(get("/api/integrations").header("X-API-KEY", "test-api-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configured").value(true))
                .andExpect(jsonPath("$.data[0].maskedSecret").value("ntn_…ABC"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("fromEnv");
    }
}
