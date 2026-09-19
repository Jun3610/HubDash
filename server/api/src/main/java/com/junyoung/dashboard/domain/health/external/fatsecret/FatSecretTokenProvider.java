package com.junyoung.dashboard.domain.health.external.fatsecret;

import com.junyoung.dashboard.domain.health.external.fatsecret.FatSecretException.Reason;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

// OAuth2 client_credentials 토큰을 발급·캐싱한다(FatSecret 토큰 유효기간은 24시간).
// 만료 직전에 요청이 몰려 만료된 토큰이 쓰이지 않도록 EXPIRY_MARGIN만큼 일찍 갱신한다.
@Component
public class FatSecretTokenProvider {

    private static final Duration EXPIRY_MARGIN = Duration.ofSeconds(60);

    private final FatSecretProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    private String cachedToken;
    private Instant expiresAt = Instant.MIN;

    @Autowired // 생성자가 둘이라(테스트용 Clock 주입 버전) 스프링이 쓸 생성자를 명시해야 한다.
    public FatSecretTokenProvider(FatSecretProperties properties, RestClient fatSecretRestClient,
                                   ObjectMapper objectMapper) {
        this(properties, fatSecretRestClient, objectMapper, Clock.systemUTC());
    }

    FatSecretTokenProvider(FatSecretProperties properties, RestClient restClient, ObjectMapper objectMapper,
                           Clock clock) {
        this.properties = properties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public synchronized String getToken() {
        if (cachedToken == null || !clock.instant().isBefore(expiresAt)) {
            fetchToken();
        }
        return cachedToken;
    }

    // 업스트림이 토큰을 거부(code 13 등)했을 때 다음 getToken()이 새로 발급하게 한다.
    public synchronized void invalidate() {
        cachedToken = null;
        expiresAt = Instant.MIN;
    }

    private void fetchToken() {
        if (!properties.isConfigured()) {
            throw new FatSecretException(Reason.NOT_CONFIGURED,
                    "FATSECRET_CLIENT_ID/FATSECRET_CLIENT_SECRET 환경변수가 설정되지 않았습니다");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "basic");

        String body;
        try {
            body = restClient.post()
                    .uri(properties.tokenUrl())
                    .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            // 잘못된 자격증명은 HTTP 400 {"error":"invalid_client"}로 온다(실제 호출로 확인).
            if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 401) {
                throw new FatSecretException(Reason.AUTH_FAILED,
                        "FatSecret 클라이언트 인증에 실패했습니다(ID/Secret 확인 필요)", e);
            }
            throw new FatSecretException(Reason.UPSTREAM_ERROR,
                    "FatSecret 토큰 발급 실패: HTTP " + e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new FatSecretException(Reason.UPSTREAM_ERROR, "FatSecret 토큰 발급 요청 실패", e);
        }

        JsonNode json;
        try {
            json = objectMapper.readTree(body);
        } catch (JacksonException e) {
            throw new FatSecretException(Reason.UPSTREAM_ERROR, "FatSecret 토큰 응답을 해석할 수 없습니다", e);
        }
        JsonNode token = json.get("access_token");
        if (token == null || token.asString("").isBlank()) {
            throw new FatSecretException(Reason.UPSTREAM_ERROR, "FatSecret 토큰 응답에 access_token이 없습니다");
        }
        long expiresInSeconds = json.has("expires_in") ? json.get("expires_in").asLong(3600) : 3600;
        cachedToken = token.asString();
        expiresAt = clock.instant().plusSeconds(expiresInSeconds).minus(EXPIRY_MARGIN);
    }
}
