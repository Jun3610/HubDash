package com.junyoung.dashboard.domain.health.repository;

import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MealRecordRepositoryTest {

    @Autowired
    private MealRecordRepository mealRecordRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        MealRecord saved = mealRecordRepository.save(
                new MealRecord(LocalDateTime.of(2026, 9, 1, 8, 0), MealType.BREAKFAST, 500,
                        60.0, 20.0, 15.0, 300.0, null));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
