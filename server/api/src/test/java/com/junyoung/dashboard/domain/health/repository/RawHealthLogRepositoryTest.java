package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.RawHealthLog;
import com.junyoung.dashboard.domain.health.entity.RawStatus;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class RawHealthLogRepositoryTest {

    @Autowired
    private RawHealthLogRepository rawHealthLogRepository;

    @Test
    void savesWithPendingStatusAndAssignsIdAndTimestamps() {
        RawHealthLog saved = rawHealthLogRepository.save(
                new RawHealthLog("2026-09-17", "70.5", 7.5, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(RawStatus.PENDING);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void markProcessedClearsFailureReason() {
        RawHealthLog saved = rawHealthLogRepository.save(
                new RawHealthLog("bad-date", null, null, null));
        saved.markFailed("recordedAtRaw 파싱 실패");

        saved.markProcessed();

        assertThat(saved.getStatus()).isEqualTo(RawStatus.PROCESSED);
        assertThat(saved.getFailureReason()).isNull();
    }

    @Test
    void markFailedSetsReason() {
        RawHealthLog saved = rawHealthLogRepository.save(
                new RawHealthLog("bad-date", null, null, null));

        saved.markFailed("recordedAtRaw 파싱 실패: bad-date");

        assertThat(saved.getStatus()).isEqualTo(RawStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo("recordedAtRaw 파싱 실패: bad-date");
    }
}
