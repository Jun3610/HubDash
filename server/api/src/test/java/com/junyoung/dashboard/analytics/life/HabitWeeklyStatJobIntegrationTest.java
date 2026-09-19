package com.junyoung.dashboard.analytics.life;

import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.domain.life.entity.HabitWeeklyStat;
import com.junyoung.dashboard.domain.life.repository.HabitLogRepository;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.domain.life.repository.HabitWeeklyStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// life(HabitLog) 파일럿 주간 집계 배치 — 실제 JobOperator로 Job을 구동해 reader/processor/writer 전체 흐름을 검증한다.
// 컨텍스트에 Job 빈이 여러 개(도메인별 주간 집계) 존재하므로 @SpringBatchTest의 자동 단일 Job 주입에 의존하지 않고
// 이 테스트가 검증할 Job을 명시적으로 지정한다.
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class HabitWeeklyStatJobIntegrationTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    @Qualifier(HabitWeeklyStatJobConfig.JOB_NAME)
    private Job lifeHabitWeeklyStatJob;

    @Autowired
    private HabitRepository habitRepository;

    @BeforeEach
    void setJob() {
        jobOperatorTestUtils.setJob(lifeHabitWeeklyStatJob);
    }

    @Autowired
    private HabitLogRepository habitLogRepository;

    @Autowired
    private HabitWeeklyStatRepository habitWeeklyStatRepository;

    @Test
    void aggregatesLogsWithinTargetWeekIntoWeeklyStat() throws Exception {
        Habit habit = habitRepository.save(new Habit("독서", null));
        LocalDate weekStart = LocalDate.of(2026, 9, 14); // 월요일

        habitLogRepository.save(new HabitLog(habit, weekStart, true, null));
        habitLogRepository.save(new HabitLog(habit, weekStart.plusDays(2), false, null));
        habitLogRepository.save(new HabitLog(habit, weekStart.plusDays(6), true, null));
        // 대상 주 밖의 로그 — 집계에 포함되면 안 됨.
        habitLogRepository.save(new HabitLog(habit, weekStart.minusDays(1), true, null));
        habitLogRepository.save(new HabitLog(habit, weekStart.plusDays(7), true, null));

        JobExecution execution = jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        HabitWeeklyStat stat = habitWeeklyStatRepository.findByHabitIdAndWeekStart(habit.getId(), weekStart)
                .orElseThrow();
        assertThat(stat.getTotalCount()).isEqualTo(3);
        assertThat(stat.getCompletedCount()).isEqualTo(2);
    }

    @Test
    void rerunningSameWeekUpdatesExistingStatInsteadOfCreatingDuplicate() throws Exception {
        Habit habit = habitRepository.save(new Habit("스트레칭", null));
        LocalDate weekStart = LocalDate.of(2026, 8, 3);

        habitLogRepository.save(new HabitLog(habit, weekStart, true, null));
        jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        HabitWeeklyStat firstRun = habitWeeklyStatRepository.findByHabitIdAndWeekStart(habit.getId(), weekStart)
                .orElseThrow();
        assertThat(firstRun.getTotalCount()).isEqualTo(1);
        Long statId = firstRun.getId();

        // 같은 주에 로그가 하나 더 생긴 뒤 재실행(run.id만 다름) — 새 행이 아니라 기존 행이 갱신돼야 한다.
        habitLogRepository.save(new HabitLog(habit, weekStart.plusDays(1), false, null));
        jobOperatorTestUtils.startJob(weekParams(weekStart, 2L));

        HabitWeeklyStat secondRun = habitWeeklyStatRepository.findByHabitIdAndWeekStart(habit.getId(), weekStart)
                .orElseThrow();
        assertThat(secondRun.getId()).isEqualTo(statId);
        assertThat(secondRun.getTotalCount()).isEqualTo(2);
        assertThat(secondRun.getCompletedCount()).isEqualTo(1);
    }

    @Test
    void habitWithNoLogsInTargetWeekProducesNoStat() throws Exception {
        Habit habit = habitRepository.save(new Habit("명상", null));
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        // 이 주가 아닌 다른 주에만 로그가 있음.
        habitLogRepository.save(new HabitLog(habit, weekStart.minusWeeks(1), true, null));

        jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        Optional<HabitWeeklyStat> stat = habitWeeklyStatRepository.findByHabitIdAndWeekStart(habit.getId(), weekStart);
        assertThat(stat).isEmpty();
    }

    private JobParameters weekParams(LocalDate weekStart, long runId) {
        return new JobParametersBuilder()
                .addString("weekStart", weekStart.toString())
                .addLong("run.id", runId)
                .toJobParameters();
    }
}
