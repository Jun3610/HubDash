package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.HealthLog;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class HealthLogRepositoryTest {

    @Autowired
    private HealthLogRepository healthLogRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        HealthLog saved = healthLogRepository.save(
                new HealthLog(LocalDateTime.of(2026, 9, 1, 7, 30), 70.5, 7.5, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
