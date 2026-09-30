package com.junyoung.dashboard.domain.integration.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 연동 계정 정보 한 건 (서비스마다 하나, 이슈 #227).
 * 비밀값은 암호문(secretEnc)으로만 들고 있고, 풀어서 쓰는 건 CredentialStore만 한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "integration_credential")
public class IntegrationCredential extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, unique = true)
    private IntegrationProvider provider;

    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "secret_enc", columnDefinition = "TEXT")
    private String secretEnc;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    public IntegrationCredential(IntegrationProvider provider) {
        this.provider = provider;
    }

    public void save(String configJson, String secretEnc, LocalDateTime verifiedAt) {
        this.configJson = configJson;
        this.secretEnc = secretEnc;
        this.lastVerifiedAt = verifiedAt;
        this.lastError = null;
    }

    public void markVerified(LocalDateTime verifiedAt) {
        this.lastVerifiedAt = verifiedAt;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.lastError = error == null ? null : error.length() <= 500 ? error : error.substring(0, 500);
    }
}
