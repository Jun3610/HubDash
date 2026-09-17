package com.junyoung.dashboard.pipeline.scheduler;

import com.junyoung.dashboard.analytics.life.LifeHabitWeeklyStatBatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsBatchScheduler.class);

    private final LifeHabitWeeklyStatBatchService lifeHabitWeeklyStatBatchService;

    public AnalyticsBatchScheduler(LifeHabitWeeklyStatBatchService lifeHabitWeeklyStatBatchService) {
        this.lifeHabitWeeklyStatBatchService = lifeHabitWeeklyStatBatchService;
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
}
