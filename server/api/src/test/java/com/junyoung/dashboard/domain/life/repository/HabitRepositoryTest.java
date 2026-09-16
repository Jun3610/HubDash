package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class HabitRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitLogRepository habitLogRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        Habit saved = habitRepository.save(new Habit("아침 스트레칭", "매일 아침 10분"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void deletingHabitCascadesToHabitLogs() {
        Habit habit = entityManager.persistAndFlush(new Habit("아침 스트레칭", "매일 아침 10분"));
        entityManager.persistAndFlush(new HabitLog(habit, LocalDate.of(2026, 9, 16), true, "완료"));
        entityManager.clear();

        Habit reloaded = habitRepository.findById(habit.getId()).orElseThrow();
        habitRepository.delete(reloaded);
        entityManager.flush();

        assertThat(habitLogRepository.count()).isZero();
    }
}
