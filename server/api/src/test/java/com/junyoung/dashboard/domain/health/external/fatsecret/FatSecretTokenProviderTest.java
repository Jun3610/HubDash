package com.junyoung.dashboard.domain.health.external.fatsecret;

import com.junyoung.dashboard.domain.health.external.fatsecret.FatSecretException.Reason;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FatSecretTokenProviderTest {

    private static final String TOKEN_URL = "https://oauth.test/connect/token";
    // "test-id:test-secret"의 Base64
    private static final String BASIC = "Basic dGVzdC1pZDp0ZXN0LXNlY3JldA==";

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private MockRestServiceServer server;
    private MutableClock clock;
    private FatSecretTokenProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        clock = new MutableClock(Instant.parse("2026-09-19T00:00:00Z"));
        provider = newProvider("test-id", "test-secret", builder.build());
    }

    private FatSecretTokenProvider newProvider(String id, String secret, RestClient client) {
        return new FatSecretTokenProvider(
                new FatSecretProperties(id, secret, TOKEN_URL, "https://api.test/rest"), client, objectMapper, clock);
    }

    private static String tokenBody(String token, long expiresIn) {
        return "{\"access_token\":\"" + token + "\",\"expires_in\":" + expiresIn
                + ",\"token_type\":\"Bearer\",\"scope\":\"basic\"}";
    }

    @Test
    void requestsClientCredentialsTokenWithBasicAuth() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", BASIC))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(org.springframework.util.CollectionUtils.toMultiValueMap(
                        java.util.Map.of("grant_type", java.util.List.of("client_credentials"),
                                "scope", java.util.List.of("basic")))))
                .andRespond(withSuccess(tokenBody("tok-1", 86400), MediaType.APPLICATION_JSON));

        assertThat(provider.getToken()).isEqualTo("tok-1");
        server.verify();
    }

    @Test
    void cachesTokenUntilItNearsExpiry() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(tokenBody("tok-1", 3600), MediaType.APPLICATION_JSON));

        assertThat(provider.getToken()).isEqualTo("tok-1");
        clock.advanceSeconds(3600 - 61); // 만료 61초 전 — 아직 캐시 사용
        assertThat(provider.getToken()).isEqualTo("tok-1");
        server.verify(); // 요청은 정확히 1번
    }

    @Test
    void refreshesTokenWhenWithinExpiryMargin() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(tokenBody("tok-1", 3600), MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(tokenBody("tok-2", 3600), MediaType.APPLICATION_JSON));

        assertThat(provider.getToken()).isEqualTo("tok-1");
        clock.advanceSeconds(3600 - 59); // 만료 59초 전 — 여유 60초 안이므로 재발급
        assertThat(provider.getToken()).isEqualTo("tok-2");
        server.verify();
    }

    @Test
    void invalidateForcesNewTokenOnNextCall() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(tokenBody("tok-1", 86400), MediaType.APPLICATION_JSON));
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess(tokenBody("tok-2", 86400), MediaType.APPLICATION_JSON));

        assertThat(provider.getToken()).isEqualTo("tok-1");
        provider.invalidate();
        assertThat(provider.getToken()).isEqualTo("tok-2");
        server.verify();
    }

    @Test
    void invalidClientCredentialsBecomeAuthFailed() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid_client\"}"));

        assertThatThrownBy(() -> provider.getToken())
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.AUTH_FAILED));
    }

    @Test
    void serverErrorBecomesUpstreamError() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        assertThatThrownBy(() -> provider.getToken())
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UPSTREAM_ERROR));
    }

    @Test
    void responseWithoutAccessTokenBecomesUpstreamError() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess("{\"expires_in\":86400}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.getToken())
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UPSTREAM_ERROR));
    }

    @Test
    void nonJsonResponseBecomesUpstreamError() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess("<html>oops</html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> provider.getToken())
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.UPSTREAM_ERROR));
    }

    @Test
    void blankCredentialsFailWithoutCallingUpstream() {
        FatSecretTokenProvider unconfigured = newProvider("", "", RestClient.builder().build());

        assertThatThrownBy(unconfigured::getToken)
                .isInstanceOfSatisfying(FatSecretException.class,
                        e -> assertThat(e.getReason()).isEqualTo(Reason.NOT_CONFIGURED));
    }

    private static class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant start) {
            this.now = start;
        }

        void advanceSeconds(long seconds) {
            now = now.plusSeconds(seconds);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
