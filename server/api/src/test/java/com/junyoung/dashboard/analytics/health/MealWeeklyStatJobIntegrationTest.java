package com.junyoung.dashboard.analytics.health;

import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.domain.health.entity.MealWeeklyStat;
import com.junyoung.dashboard.domain.health.repository.MealItemRepository;
import com.junyoung.dashboard.domain.health.repository.MealRecordRepository;
import com.junyoung.dashboard.domain.health.repository.MealWeeklyStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

// 식사 하루 합계의 주간 일평균 집계. MealRecord는 전역 테이블이라 테스트마다 서로 다른 주를 쓴다.
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class MealWeeklyStatJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(MealWeeklyStatJobConfig.JOB_NAME)
    private Job mealWeeklyStatJob;

    @BeforeEach
    void setJob() {
        jobLauncherTestUtils.setJob(mealWeeklyStatJob);
    }

    @Autowired
    private MealRecordRepository mealRecordRepository;

    @Autowired
    private MealItemRepository mealItemRepository;

    @Autowired
    private MealWeeklyStatRepository mealWeeklyStatRepository;

    @Test
    void averagesDailyTotalsOverOnlyTheDaysThatHaveRecordedItems() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 9, 14); // 월요일
        // 월: 아침 400 + 점심 600 = 1000kcal (같은 날 여러 끼니는 합산)
        addMeal(weekStart.atTime(8, 0), MealType.BREAKFAST, item("밥", 400, 60.0, 10.0, 5.0));
        addMeal(weekStart.atTime(12, 0), MealType.LUNCH, item("라면", 600, 90.0, 12.0, 20.0));
        // 수: 1400kcal
        addMeal(weekStart.plusDays(2).atTime(19, 0), MealType.DINNER, item("고기", 1400, 10.0, 80.0, 100.0));
        // 금: 끼니만 만들고 음식은 안 적음 — "기록한 날"이 아니므로 평균에 0으로 들어가면 안 됨
        addMeal(weekStart.plusDays(4).atTime(8, 0), MealType.BREAKFAST);

        JobExecution execution = jobLauncherTestUtils.launchJob(weekParams(weekStart, 1L));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        MealWeeklyStat stat = mealWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow();
        assertThat(stat.getDayCount()).isEqualTo(2);
        assertThat(stat.getAvgCalories()).isCloseTo(1200.0, within(0.001)); // (1000 + 1400) / 2
        assertThat(stat.getAvgCarbsG()).isCloseTo(80.0, within(0.001));     // (150 + 10) / 2
        assertThat(stat.getAvgProteinG()).isCloseTo(51.0, within(0.001));   // (22 + 80) / 2
        assertThat(stat.getAvgFatG()).isCloseTo(62.5, within(0.001));       // (25 + 100) / 2
    }

    @Test
    void weekBoundariesAreMondayMidnightInclusiveAndNextMondayMidnightExclusive() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 8, 3);
        addMeal(weekStart.minusDays(1).atTime(23, 59, 59), MealType.SNACK, item("전주 일요일", 9000, null, null, null));
        addMeal(weekStart.atStartOfDay(), MealType.BREAKFAST, item("월요일 00:00 포함", 500, null, null, null));
        addMeal(weekStart.plusDays(6).atTime(23, 59, 59), MealType.SNACK, item("일요일 끝 포함", 700, null, null, null));
        addMeal(weekStart.plusDays(7).atStartOfDay(), MealType.BREAKFAST, item("다음 월요일 제외", 9000, null, null, null));

        jobLauncherTestUtils.launchJob(weekParams(weekStart, 1L));

        MealWeeklyStat stat = mealWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow();
        assertThat(stat.getDayCount()).isEqualTo(2);
        assertThat(stat.getAvgCalories()).isCloseTo(600.0, within(0.001)); // (500 + 700) / 2
    }

    @Test
    void itemsWithMissingMacrosStillCountTowardCaloriesAndAreZeroForMacros() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        addMeal(weekStart.atTime(12, 0), MealType.LUNCH,
                item("라면", 520, 83.0, 11.0, 16.0), item("김치", 15, null, null, null));

        jobLauncherTestUtils.launchJob(weekParams(weekStart, 1L));

        MealWeeklyStat stat = mealWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow();
        assertThat(stat.getDayCount()).isEqualTo(1);
        assertThat(stat.getAvgCalories()).isCloseTo(535.0, within(0.001));
        assertThat(stat.getAvgCarbsG()).isCloseTo(83.0, within(0.001));
    }

    @Test
    void rerunningSameWeekUpdatesExistingStatInsteadOfCreatingDuplicate() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 6, 1);
        addMeal(weekStart.atTime(8, 0), MealType.BREAKFAST, item("밥", 400, 60.0, 10.0, 5.0));
        jobLauncherTestUtils.launchJob(weekParams(weekStart, 1L));
        Long statId = mealWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow().getId();

        addMeal(weekStart.plusDays(1).atTime(8, 0), MealType.BREAKFAST, item("죽", 200, 30.0, 5.0, 2.0));
        jobLauncherTestUtils.launchJob(weekParams(weekStart, 2L));

        MealWeeklyStat secondRun = mealWeeklyStatRepository.findByWeekStart(weekStart).orElseThrow();
        assertThat(secondRun.getId()).isEqualTo(statId);
        assertThat(secondRun.getDayCount()).isEqualTo(2);
        assertThat(secondRun.getAvgCalories()).isCloseTo(300.0, within(0.001));
    }

    @Test
    void weekWithoutAnyItemsProducesNoStatEvenIfMealRecordsExist() throws Exception {
        LocalDate weekStart = LocalDate.of(2026, 5, 4);
        addMeal(weekStart.atTime(8, 0), MealType.BREAKFAST); // 음식 없는 끼니만 존재
        addMeal(weekStart.minusWeeks(1).atTime(8, 0), MealType.BREAKFAST, item("지난주 밥", 400, null, null, null));

        jobLauncherTestUtils.launchJob(weekParams(weekStart, 1L));

        Optional<MealWeeklyStat> stat = mealWeeklyStatRepository.findByWeekStart(weekStart);
        assertThat(stat).isEmpty();
    }

    private record Item(String name, int calories, Double carbs, Double protein, Double fat) {
    }

    private static Item item(String name, int calories, Double carbs, Double protein, Double fat) {
        return new Item(name, calories, carbs, protein, fat);
    }

    private void addMeal(LocalDateTime at, MealType type, Item... items) {
        MealRecord meal = mealRecordRepository.save(new MealRecord(at, type, null));
        for (Item item : items) {
            mealItemRepository.save(new MealItem(meal, item.name(), item.calories(),
                    item.carbs(), item.protein(), item.fat(), null));
        }
    }

    private JobParameters weekParams(LocalDate weekStart, long runId) {
        return new JobParametersBuilder()
                .addString("weekStart", weekStart.toString())
                .addLong("run.id", runId)
                .toJobParameters();
    }
}
