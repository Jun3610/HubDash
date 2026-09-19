package com.junyoung.dashboard.analytics.health;

import com.junyoung.dashboard.domain.health.dto.MealTotals;
import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealWeeklyStat;
import com.junyoung.dashboard.domain.health.repository.MealItemRepository;
import com.junyoung.dashboard.domain.health.repository.MealRecordRepository;
import com.junyoung.dashboard.domain.health.repository.MealWeeklyStatRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

// 식사 하루 합계(이슈 #69)의 주간 일평균 집계 — health(HealthLog) 주간 집계(이슈 #61)처럼 부모 엔티티가 없어
// "대상 주" 자체가 처리 단위다. 음식 항목이 하나도 없는 주는 통계 행을 만들지 않는다.
@Configuration
public class MealWeeklyStatJobConfig {

    public static final String JOB_NAME = "mealWeeklyStatJob";
    private static final int CHUNK_SIZE = 10;

    private final MealRecordRepository mealRecordRepository;
    private final MealItemRepository mealItemRepository;
    private final MealWeeklyStatRepository mealWeeklyStatRepository;

    public MealWeeklyStatJobConfig(MealRecordRepository mealRecordRepository,
                                    MealItemRepository mealItemRepository,
                                    MealWeeklyStatRepository mealWeeklyStatRepository) {
        this.mealRecordRepository = mealRecordRepository;
        this.mealItemRepository = mealItemRepository;
        this.mealWeeklyStatRepository = mealWeeklyStatRepository;
    }

    @Bean(JOB_NAME)
    public Job mealWeeklyStatJob(JobRepository jobRepository, Step mealWeeklyStatStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(mealWeeklyStatStep)
                .build();
    }

    @Bean
    public Step mealWeeklyStatStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    ItemReader<LocalDate> mealWeekReader,
                                    ItemProcessor<LocalDate, MealWeeklyStat> mealWeeklyStatProcessor,
                                    ItemWriter<MealWeeklyStat> mealWeeklyStatWriter) {
        return new StepBuilder("mealWeeklyStatStep", jobRepository)
                .<LocalDate, MealWeeklyStat>chunk(CHUNK_SIZE)
                .reader(mealWeekReader)
                .processor(mealWeeklyStatProcessor)
                .writer(mealWeeklyStatWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<LocalDate> mealWeekReader(@Value("#{jobParameters['weekStart']}") String weekStartParam) {
        LocalDate weekStart = LocalDate.parse(weekStartParam);
        boolean hasItems = mealItemRepository.countByConsumedAtBetween(
                weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay()) > 0;
        return new ListItemReader<>(hasItems ? List.of(weekStart) : List.of());
    }

    @Bean
    @StepScope
    public ItemProcessor<LocalDate, MealWeeklyStat> mealWeeklyStatProcessor() {
        return weekStart -> {
            // 청크 트랜잭션 안이라 끼니의 items(지연 로딩)를 읽을 수 있다. 범위는 [월요일 00:00, 다음 월요일 00:00).
            List<MealRecord> records = mealRecordRepository.findByConsumedAtGreaterThanEqualAndConsumedAtLessThan(
                    weekStart.atStartOfDay(), weekStart.plusDays(7).atStartOfDay());

            // 날짜별로 그날 먹은 모든 항목을 모은다. 항목이 없는 끼니는 그날을 "기록한 날"로 만들지 않는다.
            Map<LocalDate, List<MealItem>> itemsByDay = new TreeMap<>();
            for (MealRecord record : records) {
                if (record.getItems().isEmpty()) {
                    continue;
                }
                itemsByDay.computeIfAbsent(record.getConsumedAt().toLocalDate(), day -> new ArrayList<>())
                        .addAll(record.getItems());
            }
            if (itemsByDay.isEmpty()) {
                return null;
            }

            int dayCount = itemsByDay.size();
            double calories = 0;
            double carbs = 0;
            double protein = 0;
            double fat = 0;
            for (List<MealItem> dayItems : itemsByDay.values()) {
                MealTotals daily = MealTotals.of(dayItems);
                calories += daily.calories();
                carbs += daily.carbsG();
                protein += daily.proteinG();
                fat += daily.fatG();
            }
            double avgCalories = round(calories / dayCount);
            double avgCarbs = round(carbs / dayCount);
            double avgProtein = round(protein / dayCount);
            double avgFat = round(fat / dayCount);

            return mealWeeklyStatRepository.findByWeekStart(weekStart)
                    .map(existing -> {
                        existing.updateStats(dayCount, avgCalories, avgCarbs, avgProtein, avgFat);
                        return existing;
                    })
                    .orElseGet(() -> new MealWeeklyStat(weekStart, dayCount, avgCalories, avgCarbs, avgProtein, avgFat));
        };
    }

    @Bean
    public ItemWriter<MealWeeklyStat> mealWeeklyStatWriter() {
        return chunk -> mealWeeklyStatRepository.saveAll(chunk.getItems());
    }

    private static double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
