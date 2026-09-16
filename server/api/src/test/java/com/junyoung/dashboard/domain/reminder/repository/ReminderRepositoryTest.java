package com.junyoung.dashboard.domain.reminder.repository;

import com.junyoung.dashboard.domain.reminder.entity.Reminder;
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
class ReminderRepositoryTest {

    @Autowired
    private ReminderRepository reminderRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        Reminder saved = reminderRepository.save(new Reminder(
                "과제 마감 임박", LocalDateTime.of(2026, 9, 20, 9, 0), "pknu", 42L, false));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
