package com.junyoung.dashboard.analytics.health;

import com.junyoung.dashboard.domain.health.entity.HealthLogWeeklyStat;
import com.junyoung.dashboard.domain.health.repository.HealthLogRepository;
import com.junyoung.dashboard.domain.health.repository.HealthLogWeeklyStatRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// health(HealthLog) 주간 체중/수면 평균 집계 — life 파일럿(이슈 #54)의 패턴을 재사용하되,
// HealthLog는 부모 엔티티 없이 전역 로그 하나뿐이라 "대상 주" 자체가 처리 단위(아이템)가 된다.
@Configuration
public class HealthLogWeeklyStatJobConfig {

    public static final String JOB_NAME = "healthLogWeeklyStatJob";
    private static final int CHUNK_SIZE = 10;

    private final HealthLogRepository healthLogRepository;
    private final HealthLogWeeklyStatRepository healthLogWeeklyStatRepository;

    public HealthLogWeeklyStatJobConfig(HealthLogRepository healthLogRepository,
                                         HealthLogWeeklyStatRepository healthLogWeeklyStatRepository) {
        this.healthLogRepository = healthLogRepository;
        this.healthLogWeeklyStatRepository = healthLogWeeklyStatRepository;
    }

    @Bean(JOB_NAME)
    public Job healthLogWeeklyStatJob(JobRepository jobRepository, Step healthLogWeeklyStatStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(healthLogWeeklyStatStep)
                .build();
    }

    @Bean
    public Step healthLogWeeklyStatStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         ItemReader<LocalDate> healthWeekReader,
                                         ItemProcessor<LocalDate, HealthLogWeeklyStat> healthLogWeeklyStatProcessor,
                                         ItemWriter<HealthLogWeeklyStat> healthLogWeeklyStatWriter) {
        return new StepBuilder("healthLogWeeklyStatStep", jobRepository)
                .<LocalDate, HealthLogWeeklyStat>chunk(CHUNK_SIZE)
                .reader(healthWeekReader)
                .processor(healthLogWeeklyStatProcessor)
                .writer(healthLogWeeklyStatWriter)
                .transactionManager(transactionManager)
                .build();
    }

    // 대상 주에 로그가 하나도 없으면 빈 리더 → 통계 행을 만들지 않는다.
    @Bean
    @StepScope
    public ItemReader<LocalDate> healthWeekReader(@Value("#{jobParameters['weekStart']}") String weekStartParam) {
        LocalDate weekStart = LocalDate.parse(weekStartParam);
        boolean hasLogs = healthLogRepository.countByRecordedAtGreaterThanEqualAndRecordedAtLessThan(
                weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay()) > 0;
        return new ListItemReader<>(hasLogs ? List.of(weekStart) : List.of());
    }

    @Bean
    @StepScope
    public ItemProcessor<LocalDate, HealthLogWeeklyStat> healthLogWeeklyStatProcessor() {
        return weekStart -> {
            LocalDateTime from = weekStart.atStartOfDay();
            LocalDateTime to = weekStart.plusDays(7).atStartOfDay();
            int logCount = (int) healthLogRepository.countByRecordedAtGreaterThanEqualAndRecordedAtLessThan(from, to);
            Double avgWeightKg = healthLogRepository.averageWeightKgBetween(from, to);
            Double avgSleepHours = healthLogRepository.averageSleepHoursBetween(from, to);

            return healthLogWeeklyStatRepository.findByWeekStart(weekStart)
                    .map(existing -> {
                        existing.updateStats(logCount, avgWeightKg, avgSleepHours);
                        return existing;
                    })
                    .orElseGet(() -> new HealthLogWeeklyStat(weekStart, logCount, avgWeightKg, avgSleepHours));
        };
    }

    @Bean
    public ItemWriter<HealthLogWeeklyStat> healthLogWeeklyStatWriter() {
        return chunk -> healthLogWeeklyStatRepository.saveAll(chunk.getItems());
    }
}
