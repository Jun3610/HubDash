package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class ReadingLogRepositoryTest {

    @Autowired
    private ReadingLogRepository readingLogRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        ReadingLog saved = readingLogRepository.save(
                new ReadingLog("클린 코드", "로버트 마틴", LocalDate.of(2026, 9, 1), null, null, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
