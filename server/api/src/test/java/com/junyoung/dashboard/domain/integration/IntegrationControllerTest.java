package com.junyoung.dashboard.domain.integration;

import com.junyoung.dashboard.domain.integration.entity.IntegrationCredential;
import com.junyoung.dashboard.domain.integration.entity.IntegrationProvider;
import com.junyoung.dashboard.domain.integration.repository.IntegrationCredentialRepository;
import com.junyoung.dashboard.global.crypto.CredentialCipher;
import com.junyoung.dashboard.global.icloud.CalDavCalendar;
import com.junyoung.dashboard.global.icloud.CalDavClient;
import com.junyoung.dashboard.global.icloud.ICloudException;
import com.junyoung.dashboard.global.icloud.ICloudLogin;
import com.junyoung.dashboard.global.notion.NotionAccount;
import com.junyoung.dashboard.global.notion.NotionClient;
import com.junyoung.dashboard.global.notion.NotionException;
import com.junyoung.dashboard.global.notion.NotionTokenSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 노션/iCloud는 가짜로 두고, 저장 → 암호화 → 응답 마스킹까지 실제 경로로 확인한다 (이슈 #227)
@SpringBootTest(properties = {
        "app.credential-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "app.notion.token=",
        "app.icloud.duplicate-calendars=ParkJunYoung_Schedule"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntegrationControllerTest {

    private static final String TOKEN = "ntn_1234567890abcdefSECRETTOKEN6SK";
    private static final String APP_PASSWORD = "abcd-efgh-ijkl-mnop";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private IntegrationCredentialRepository repository;
    @Autowired
    private CredentialCipher cipher;
    @Autowired
    private NotionTokenSource tokens;
    @MockitoBean
    private NotionClient notion;
    @MockitoBean
    private CalDavClient caldav;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) throws Exception {
        return mvc.perform(req.header("X-API-KEY", "test-api-key").contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void savesNotionTokenEncryptedAndNeverReturnsIt() throws Exception {
        when(notion.me(TOKEN)).thenReturn(new NotionAccount("bot-1", "준영 워크스페이스"));

        String body = call(put("/api/integrations/notion").content("""
                {"token":"%s","scheduleDatabase":"https://www.notion.so/3c62b9e1fd1480778cc7eb24389c1704?v=abc"}""".formatted(TOKEN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.integration.configured").value(true))
                .andExpect(jsonPath("$.data.integration.account").value("준영 워크스페이스"))
                .andExpect(jsonPath("$.data.integration.maskedSecret").value("ntn_…6SK"))
                .andExpect(jsonPath("$.data.integration.scheduleDatabaseId").value("3c62b9e1fd1480778cc7eb24389c1704"))
                .andExpect(jsonPath("$.data.accountChanged").value(false))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(TOKEN).doesNotContain("SECRETTOKEN");

        IntegrationCredential saved = repository.findByProvider(IntegrationProvider.NOTION).orElseThrow();
        assertThat(saved.getSecretEnc()).doesNotContain(TOKEN);
        assertThat(cipher.decrypt(saved.getSecretEnc())).isEqualTo(TOKEN);
        assertThat(saved.getConfigJson()).doesNotContain(TOKEN);
        // 저장 즉시 노션 호출에 쓰이는 토큰이 바뀐다 (재시작 없이)
        assertThat(tokens.currentToken()).isEqualTo(TOKEN);

        String list = call(get("/api/integrations")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].provider").value("NOTION"))
                .andExpect(jsonPath("$.data[1].provider").value("ICLOUD"))
                .andExpect(jsonPath("$.data[1].configured").value(false))
                .andReturn().getResponse().getContentAsString();
        assertThat(list).doesNotContain(TOKEN);
    }

    @Test
    void failedVerificationSavesNothing() throws Exception {
        when(notion.me(any())).thenThrow(new NotionException("노션 토큰이 올바르지 않아요"));

        call(put("/api/integrations/notion").content("{\"token\":\"ntn_wrongwrongwrong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INTEGRATION_INVALID"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("노션 토큰이 올바르지 않아요")));

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void unreadableDatabaseAddressIsRejected() throws Exception {
        call(put("/api/integrations/notion").content("{\"token\":\"" + TOKEN + "\",\"scheduleDatabase\":\"그냥 글자\"}"))
                .andExpect(status().isBadRequest());
        verify(notion, never()).me(any());
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void blankTokenKeepsSavedTokenAndReportsAccountChange() throws Exception {
        when(notion.me(TOKEN)).thenReturn(new NotionAccount("bot-1", "A"));
        call(put("/api/integrations/notion").content("{\"token\":\"" + TOKEN + "\"}")).andExpect(status().isOk());

        // 같은 토큰(빈 값) → 계정 그대로
        call(put("/api/integrations/notion").content("{\"token\":\"\",\"scheduleDatabase\":\"3c62b9e1fd1480778cc7eb24389c1704\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountChanged").value(false))
                .andExpect(jsonPath("$.data.integration.scheduleDatabaseId").value("3c62b9e1fd1480778cc7eb24389c1704"));

        // 다른 통합의 토큰 → 계정 바뀜, 일정 DB 설정은 유지
        String other = "ntn_otherotherotherother999";
        when(notion.me(other)).thenReturn(new NotionAccount("bot-2", "B"));
        call(put("/api/integrations/notion").content("{\"token\":\"" + other + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountChanged").value(true))
                .andExpect(jsonPath("$.data.integration.account").value("B"))
                .andExpect(jsonPath("$.data.integration.scheduleDatabaseId").value("3c62b9e1fd1480778cc7eb24389c1704"));
        assertThat(tokens.currentToken()).isEqualTo(other);
    }

    @Test
    void savesICloudLoginAndCalendarsWithoutReturningPassword() throws Exception {
        when(caldav.principal(any())).thenReturn(URI.create("https://caldav.test/1/principal/"));

        String body = call(put("/api/integrations/icloud").content("""
                {"appleId":"me@icloud.com","appPassword":"%s","calendars":["집"," 학교 "]}""".formatted(APP_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.integration.configured").value(true))
                .andExpect(jsonPath("$.data.integration.account").value("me@icloud.com"))
                .andExpect(jsonPath("$.data.integration.maskedSecret").value("…nop"))
                .andExpect(jsonPath("$.data.integration.calendars[1]").value("학교"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain(APP_PASSWORD);
        verify(caldav).principal(new ICloudLogin("me@icloud.com", APP_PASSWORD));
        assertThat(repository.findByProvider(IntegrationProvider.ICLOUD).orElseThrow().getSecretEnc()).doesNotContain(APP_PASSWORD);

        // 캘린더만 바꾸면 저장된 암호로 다시 확인한다
        call(put("/api/integrations/icloud").content("{\"calendars\":[\"집\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.integration.calendars.length()").value(1))
                .andExpect(jsonPath("$.data.accountChanged").value(false));
    }

    @Test
    void wrongICloudPasswordSavesNothing() throws Exception {
        when(caldav.principal(any())).thenThrow(new ICloudException("Apple ID나 앱 암호가 올바르지 않아요"));

        call(put("/api/integrations/icloud").content("{\"appleId\":\"me@icloud.com\",\"appPassword\":\"wrong-pass-word-xx\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INTEGRATION_INVALID"));
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void calendarListMarksSelectedAndDuplicateCalendar() throws Exception {
        when(caldav.principal(any())).thenReturn(URI.create("https://caldav.test/1/principal/"));
        call(put("/api/integrations/icloud").content("""
                {"appleId":"me@icloud.com","appPassword":"%s","calendars":["집"]}""".formatted(APP_PASSWORD)));
        when(caldav.calendars(any())).thenReturn(List.of(
                new CalDavCalendar("집", "https://x/home/"),
                new CalDavCalendar("ParkJunYoung_Schedule", "https://x/pj/")));

        call(get("/api/integrations/icloud/calendars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("집"))
                .andExpect(jsonPath("$.data[0].selected").value(true))
                .andExpect(jsonPath("$.data[0].duplicateWarning").doesNotExist())
                .andExpect(jsonPath("$.data[1].selected").value(false))
                .andExpect(jsonPath("$.data[1].duplicateWarning").value(org.hamcrest.Matchers.containsString("중복")));
    }

    @Test
    void testRecordsResultAndDeleteRemovesCredentialOnly() throws Exception {
        when(notion.me(TOKEN)).thenReturn(new NotionAccount("bot-1", "A"));
        call(put("/api/integrations/notion").content("{\"token\":\"" + TOKEN + "\"}"));

        when(notion.me(TOKEN)).thenThrow(new NotionException("노션 API 오류 (500)"));
        call(post("/api/integrations/notion/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ok").value(false))
                .andExpect(jsonPath("$.data.error").value("노션 API 오류 (500)"));
        assertThat(repository.findByProvider(IntegrationProvider.NOTION).orElseThrow().getLastError())
                .isEqualTo("노션 API 오류 (500)");

        call(delete("/api/integrations/notion")).andExpect(status().isNoContent());
        assertThat(repository.findAll()).isEmpty();
        call(get("/api/integrations")).andExpect(jsonPath("$.data[0].configured").value(false));

        call(post("/api/integrations/dropbox/test")).andExpect(status().isNotFound());
    }

    @Test
    void calendarsNeedSavedAccount() throws Exception {
        call(get("/api/integrations/icloud/calendars")).andExpect(status().isBadRequest());
        verify(caldav, never()).calendars(eq(null));
    }
}
