package com.junyoung.dashboard.analytics.life;

import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitWeeklyStat;
import com.junyoung.dashboard.domain.life.repository.HabitLogRepository;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.domain.life.repository.HabitWeeklyStatRepository;
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

// life 도메인(HabitLog)을 파일럿으로 삼은 "정규화 DB → 분석 테이블" 주간 집계 배치.
// 이슈 #36(Kafka raw→ETL) 파일럿과 같은 이유로 8개 도메인을 한 번에 만들지 않고 하나부터 패턴을 검증한다.
@Configuration
public class HabitWeeklyStatJobConfig {

    public static final String JOB_NAME = "lifeHabitWeeklyStatJob";
    private static final int CHUNK_SIZE = 10;

    private final HabitLogRepository habitLogRepository;
    private final HabitRepository habitRepository;
    private final HabitWeeklyStatRepository habitWeeklyStatRepository;

    public HabitWeeklyStatJobConfig(HabitLogRepository habitLogRepository,
                                     HabitRepository habitRepository,
                                     HabitWeeklyStatRepository habitWeeklyStatRepository) {
        this.habitLogRepository = habitLogRepository;
        this.habitRepository = habitRepository;
        this.habitWeeklyStatRepository = habitWeeklyStatRepository;
    }

    @Bean(JOB_NAME)
    public Job lifeHabitWeeklyStatJob(JobRepository jobRepository, Step lifeHabitWeeklyStatStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(lifeHabitWeeklyStatStep)
                .build();
    }

    @Bean
    public Step lifeHabitWeeklyStatStep(JobRepository jobRepository,
                                         PlatformTransactionManager transactionManager,
                                         ItemReader<Long> habitIdReader,
                                         ItemProcessor<Long, HabitWeeklyStat> habitWeeklyStatProcessor,
                                         ItemWriter<HabitWeeklyStat> habitWeeklyStatWriter) {
        return new StepBuilder("lifeHabitWeeklyStatStep", jobRepository)
                .<Long, HabitWeeklyStat>chunk(CHUNK_SIZE, transactionManager)
                .reader(habitIdReader)
                .processor(habitWeeklyStatProcessor)
                .writer(habitWeeklyStatWriter)
                .build();
    }

    // weekStart(잡 파라미터)가 있는 그 주에 로그가 하나라도 있는 habitId만 대상으로 삼는다.
    @Bean
    @StepScope
    public ItemReader<Long> habitIdReader(@Value("#{jobParameters['weekStart']}") String weekStartParam) {
        LocalDate weekStart = LocalDate.parse(weekStartParam);
        LocalDate weekEnd = weekStart.plusDays(6);
        return new ListItemReader<>(habitLogRepository.findDistinctHabitIdsWithLogsBetween(weekStart, weekEnd));
    }

    // 같은 habitId+weekStart로 재실행되면(수동 재집계) 새로 만들지 않고 기존 통계 행을 갱신한다.
    @Bean
    @StepScope
    public ItemProcessor<Long, HabitWeeklyStat> habitWeeklyStatProcessor(
            @Value("#{jobParameters['weekStart']}") String weekStartParam) {
        LocalDate weekStart = LocalDate.parse(weekStartParam);
        LocalDate weekEnd = weekStart.plusDays(6);
        return habitId -> {
            Habit habit = habitRepository.findById(habitId).orElse(null);
            if (habit == null) {
                return null;
            }
            long total = habitLogRepository.countByHabitIdAndPerformedAtBetween(habitId, weekStart, weekEnd);
            long completed = habitLogRepository.countByHabitIdAndPerformedAtBetweenAndCompletedTrue(
                    habitId, weekStart, weekEnd);

            return habitWeeklyStatRepository.findByHabitIdAndWeekStart(habitId, weekStart)
                    .map(existing -> {
                        existing.updateCounts((int) total, (int) completed);
                        return existing;
                    })
                    .orElseGet(() -> new HabitWeeklyStat(habit, weekStart, (int) total, (int) completed));
        };
    }

    @Bean
    public ItemWriter<HabitWeeklyStat> habitWeeklyStatWriter() {
        return chunk -> habitWeeklyStatRepository.saveAll(chunk.getItems());
    }
}
