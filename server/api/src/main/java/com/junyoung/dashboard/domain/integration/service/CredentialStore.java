package com.junyoung.dashboard.domain.integration.service;

import com.junyoung.dashboard.domain.integration.entity.IntegrationCredential;
import com.junyoung.dashboard.domain.integration.entity.IntegrationProvider;
import com.junyoung.dashboard.domain.integration.repository.IntegrationCredentialRepository;
import com.junyoung.dashboard.global.crypto.CredentialCipher;
import com.junyoung.dashboard.global.exception.IntegrationException;
import com.junyoung.dashboard.global.icloud.ICloudLogin;
import com.junyoung.dashboard.global.notion.NotionTokenSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.Optional;

/**
 * 저장된 연동 계정 정보를 읽는 곳 (이슈 #227). 비밀값을 푸는 건 여기서만 한다.
 * 노션 토큰 우선순위: 설정 화면에서 저장한 값(DB) → 환경변수 NOTION_TOKEN → 없음.
 */
@Service
@Transactional(readOnly = true)
public class CredentialStore implements NotionTokenSource {

    static final JsonMapper JSON = JsonMapper.builder().build();

    private final IntegrationCredentialRepository repository;
    private final CredentialCipher cipher;
    private final String envNotionToken;

    public CredentialStore(IntegrationCredentialRepository repository, CredentialCipher cipher,
                           @Value("${app.notion.token:}") String envNotionToken) {
        this.repository = repository;
        this.cipher = cipher;
        this.envNotionToken = envNotionToken == null || envNotionToken.isBlank() ? null : envNotionToken.strip();
    }

    public Optional<IntegrationCredential> find(IntegrationProvider provider) {
        return repository.findByProvider(provider);
    }

    /** 노션 API 호출마다 불린다 — 저장값을 못 풀면(키가 바뀜) 환경변수로 넘어간다 */
    @Override
    public String currentToken() {
        String stored = find(IntegrationProvider.NOTION).map(this::secretOrNull).orElse(null);
        return stored != null ? stored : envNotionToken;
    }

    /** 저장값 없이 환경변수 토큰만 쓰고 있는지 */
    public boolean notionTokenFromEnv() {
        return envNotionToken != null && find(IntegrationProvider.NOTION).map(IntegrationCredential::getSecretEnc).isEmpty();
    }

    public NotionSettings notionSettings() {
        return find(IntegrationProvider.NOTION)
                .map(c -> read(c.getConfigJson(), NotionSettings.class, NotionSettings.EMPTY))
                .orElse(NotionSettings.EMPTY);
    }

    public ICloudSettings icloudSettings() {
        return find(IntegrationProvider.ICLOUD)
                .map(c -> read(c.getConfigJson(), ICloudSettings.class, ICloudSettings.EMPTY))
                .orElse(ICloudSettings.EMPTY);
    }

    /** 저장된 iCloud 로그인 — 없으면 null. 앱 암호를 못 풀면 IntegrationException */
    public ICloudLogin icloudLogin() {
        Optional<IntegrationCredential> c = find(IntegrationProvider.ICLOUD);
        if (c.isEmpty() || c.get().getSecretEnc() == null) {
            return null;
        }
        String appleId = icloudSettings().appleId();
        return appleId == null ? null : new ICloudLogin(appleId, cipher.decrypt(c.get().getSecretEnc()));
    }

    /** 저장된 비밀값을 풀어서 — 없으면 null */
    String secret(IntegrationProvider provider) {
        return find(provider).map(IntegrationCredential::getSecretEnc).map(cipher::decrypt).orElse(null);
    }

    String envNotionToken() {
        return envNotionToken;
    }

    private String secretOrNull(IntegrationCredential c) {
        if (c.getSecretEnc() == null || !cipher.ready()) {
            return null;
        }
        try {
            return cipher.decrypt(c.getSecretEnc());
        } catch (IntegrationException e) {
            return null;
        }
    }

    static String write(Object settings) {
        return JSON.writeValueAsString(settings);
    }

    private static <T> T read(String json, Class<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        return JSON.readValue(json, type);
    }
}
