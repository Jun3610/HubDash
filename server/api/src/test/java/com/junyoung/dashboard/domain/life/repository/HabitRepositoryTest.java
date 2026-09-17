package com.junyoung.dashboard.domain.life.repository;

import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
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
        Long habitId = reloaded.getId();
        habitRepository.delete(reloaded);
        entityManager.flush();

        // 전역 count()는 같은 H2 인스턴스를 공유하는 다른 테스트(예: RawHabitLogConsumerIntegrationTest)가
        // 커밋한 다른 Habit의 HabitLog까지 집계해 깨질 수 있다 — 이 habitId로 범위를 좁혀서 검증한다.
        assertThat(habitLogRepository.findByHabitId(habitId, Pageable.unpaged()).getTotalElements()).isZero();
    }
}
