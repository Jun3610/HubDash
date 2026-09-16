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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class HabitLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private HabitLogRepository habitLogRepository;

    @Test
    void findsLogsByHabitId() {
        Habit habit = entityManager.persistAndFlush(new Habit("아침 스트레칭", null));
        entityManager.persistAndFlush(new HabitLog(habit, LocalDate.of(2026, 9, 16), true, "완료"));

        List<HabitLog> logs = habitLogRepository.findByHabitId(habit.getId());

        assertThat(logs).hasSize(1);
        assertThat(logs.get(0).getCompleted()).isTrue();
    }
}
