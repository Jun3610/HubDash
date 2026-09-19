package com.junyoung.dashboard.analytics.health;

import com.junyoung.dashboard.domain.health.entity.HealthLog;
import com.junyoung.dashboard.domain.health.entity.HealthLogWeeklyStat;
import com.junyoung.dashboard.domain.health.repository.HealthLogRepository;
import com.junyoung.dashboard.domain.health.repository.HealthLogWeeklyStatRepository;
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
import static org.assertj.core.api.Assertions.within;

// health(HealthLog) 주간 체중/수면 평균 집계 — life 파일럿(이슈 #54)과 같은 방식으로 실제 JobOperator로 검증한다.
// HealthLog는 전역 테이블이라 테스트끼리 데이터가 섞이지 않도록 테스트마다 서로 다른 주를 사용한다.
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class HealthLogWeeklyStatJobIntegrationTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    @Qualifier(HealthLogWeeklyStatJobConfig.JOB_NAME)
    private Job healthLogWeeklyStatJob;

    @BeforeEach
    void setJob() {
        jobOperatorTestUtils.setJob(healthLogWeeklyStatJob);
    }

    @Autowired
    private HealthLogRepository healthLogRepository;

    @Autowired
    private HealthLogWeeklyStatRepository healthLogWeeklyStatRepository;

    @Test
    void averagesWeightAndSleepWithinTargetWeek() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 9, 14); // 월요일

        healthLogRepository.save(new HealthLog(weekStart, 70.0, 6.0, null));
        healthLogRepository.save(new HealthLog(weekStart.plusDays(3), 72.0, 8.0, null));
        // 대상 주 밖 — 집계에 포함되면 안 됨.
        healthLogRepository.save(new HealthLog(weekStart.minusDays(1), 100.0, 1.0, null));
        healthLogRepository.save(new HealthLog(weekStart.plusDays(7), 100.0, 1.0, null));

        JobExecution execution = jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        HealthLogWeeklyStat stat = healthLogWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow();
        assertThat(stat.getLogCount()).isEqualTo(2);
        assertThat(stat.getAvgWeightKg()).isCloseTo(71.0, within(0.001));
        assertThat(stat.getAvgSleepHours()).isCloseTo(7.0, within(0.001));
    }

    @Test
    void ignoresNullValuesWhenAveragingAndKeepsNullIfNoValueAtAll() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 8, 3);

        // 체중은 한 건만 있고, 수면은 전부 null인 주.
        healthLogRepository.save(new HealthLog(weekStart, 68.0, null, null));
        healthLogRepository.save(new HealthLog(weekStart.plusDays(1), null, null, "체중 미측정"));

        jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        HealthLogWeeklyStat stat = healthLogWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow();
        assertThat(stat.getLogCount()).isEqualTo(2);
        assertThat(stat.getAvgWeightKg()).isCloseTo(68.0, within(0.001));
        assertThat(stat.getAvgSleepHours()).isNull();
    }

    @Test
    void rerunningSameWeekUpdatesExistingStatInsteadOfCreatingDuplicate() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);

        healthLogRepository.save(new HealthLog(weekStart, 70.0, 7.0, null));
        jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        Long statId = healthLogWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow().getId();

        healthLogRepository.save(new HealthLog(weekStart.plusDays(1), 72.0, 5.0, null));
        jobOperatorTestUtils.startJob(weekParams(weekStart, 2L));

        HealthLogWeeklyStat secondRun = healthLogWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow();
        assertThat(secondRun.getId()).isEqualTo(statId);
        assertThat(secondRun.getLogCount()).isEqualTo(2);
        assertThat(secondRun.getAvgWeightKg()).isCloseTo(71.0, within(0.001));
        assertThat(secondRun.getAvgSleepHours()).isCloseTo(6.0, within(0.001));
    }

    @Test
    void weekWithNoLogsProducesNoStat() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        healthLogRepository.save(new HealthLog(weekStart.minusWeeks(1), 70.0, 7.0, null));

        jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        Optional<HealthLogWeeklyStat> stat = healthLogWeeklyStatRepository.findByWeekStart(weekStart);
        assertThat(stat).isEmpty();
    }

    private JobParameters weekParams(LocalDate weekStart, long runId) {
        return new JobParametersBuilder()
                .addString("weekStart", weekStart.toString())
                .addLong("run.id", runId)
                .toJobParameters();
    }
}
