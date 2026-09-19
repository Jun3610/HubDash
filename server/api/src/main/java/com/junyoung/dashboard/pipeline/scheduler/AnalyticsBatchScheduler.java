package com.junyoung.dashboard.pipeline.scheduler;

import com.junyoung.dashboard.analytics.health.HealthLogWeeklyStatBatchService;
import com.junyoung.dashboard.analytics.health.MealWeeklyStatBatchService;
import com.junyoung.dashboard.analytics.life.LifeHabitWeeklyStatBatchService;
import com.junyoung.dashboard.analytics.pknu.AssignmentWeeklyStatBatchService;
import com.junyoung.dashboard.analytics.study.StudyTopicWeeklyStatBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsBatchScheduler.class);

    private final LifeHabitWeeklyStatBatchService lifeHabitWeeklyStatBatchService;
    private final StudyTopicWeeklyStatBatchService studyTopicWeeklyStatBatchService;
    private final HealthLogWeeklyStatBatchService healthLogWeeklyStatBatchService;
    private final AssignmentWeeklyStatBatchService assignmentWeeklyStatBatchService;
    private final MealWeeklyStatBatchService mealWeeklyStatBatchService;

    public AnalyticsBatchScheduler(LifeHabitWeeklyStatBatchService lifeHabitWeeklyStatBatchService,
                                    StudyTopicWeeklyStatBatchService studyTopicWeeklyStatBatchService,
                                    HealthLogWeeklyStatBatchService healthLogWeeklyStatBatchService,
                                    AssignmentWeeklyStatBatchService assignmentWeeklyStatBatchService,
                                    MealWeeklyStatBatchService mealWeeklyStatBatchService) {
        this.lifeHabitWeeklyStatBatchService = lifeHabitWeeklyStatBatchService;
        this.studyTopicWeeklyStatBatchService = studyTopicWeeklyStatBatchService;
        this.healthLogWeeklyStatBatchService = healthLogWeeklyStatBatchService;
        this.assignmentWeeklyStatBatchService = assignmentWeeklyStatBatchService;
        this.mealWeeklyStatBatchService = mealWeeklyStatBatchService;
    }

    // 매주 월요일 00:10에 방금 끝난 주(지난주)를 집계한다.
    @Scheduled(cron = "0 10 0 * * MON")
    public void runLifeHabitWeeklyStat() {
        try {
            lifeHabitWeeklyStatBatchService.run(null);
        } catch (Exception e) {
            log.error("lifeHabitWeeklyStatJob 스케줄 실행 실패", e);
        }
    }

    @Scheduled(cron = "0 10 0 * * MON")
    public void runStudyTopicWeeklyStat() {
        try {
            studyTopicWeeklyStatBatchService.run(null);
        } catch (Exception e) {
            log.error("studyTopicWeeklyStatJob 스케줄 실행 실패", e);
        }
    }

    @Scheduled(cron = "0 10 0 * * MON")
    public void runHealthLogWeeklyStat() {
        try {
            healthLogWeeklyStatBatchService.run(null);
        } catch (Exception e) {
            log.error("healthLogWeeklyStatJob 스케줄 실행 실패", e);
        }
    }

    @Scheduled(cron = "0 10 0 * * MON")
    public void runAssignmentWeeklyStat() {
        try {
            assignmentWeeklyStatBatchService.run(null);
        } catch (Exception e) {
            log.error("assignmentWeeklyStatJob 스케줄 실행 실패", e);
        }
    }

    @Scheduled(cron = "0 10 0 * * MON")
    public void runMealWeeklyStat() {
        try {
            mealWeeklyStatBatchService.run(null);
        } catch (Exception e) {
            log.error("mealWeeklyStatJob 스케줄 실행 실패", e);
        }
    }
}
