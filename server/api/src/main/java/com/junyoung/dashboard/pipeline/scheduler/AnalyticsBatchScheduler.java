package com.junyoung.dashboard.pipeline.scheduler;

import com.junyoung.dashboard.analytics.health.HealthLogWeeklyStatJobConfig;
import com.junyoung.dashboard.analytics.health.MealWeeklyStatJobConfig;
import com.junyoung.dashboard.analytics.life.HabitWeeklyStatJobConfig;
import com.junyoung.dashboard.analytics.pknu.AssignmentWeeklyStatJobConfig;
import com.junyoung.dashboard.analytics.study.StudyTopicWeeklyStatJobConfig;
import com.junyoung.dashboard.pipeline.launcher.WeeklyStatJobRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 매일 00:10에 "방금 끝난 주(지난주)"를 재집계한다. 모든 주간 Job이 같은 주를 여러 번 돌려도 같은 행을 갱신하는(upsert)
// 멱등 Job이라, 월요일 한 번만 돌리는 것보다 매일 돌리는 편이 안전하다 — 월요일 실행이 실패했거나 앱이 꺼져 있었어도
// 다음 날 스스로 복구되고, 월요일에 늦게 입력한 지난주 기록도 반영된다. cron은 analytics.batch.cron으로 바꿀 수 있다.
// 서버를 필요할 때만 켜서 쓰므로(#93) 00:10에 꺼져 있으면 매일 실행이 한 번도 돌지 않는다 — 그래서 앱이 뜰 때도
// 한 번 집계한다(#128). analytics.batch.run-on-startup=false로 끌 수 있다(테스트 프로파일은 끔).
@Component
public class AnalyticsBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsBatchScheduler.class);
    private static final String CRON = "${analytics.batch.cron:0 10 0 * * *}";

    private final WeeklyStatJobRunner runner;

    private final boolean runOnStartup;

    public AnalyticsBatchScheduler(WeeklyStatJobRunner runner,
                                   @Value("${analytics.batch.run-on-startup:true}") boolean runOnStartup) {
        this.runner = runner;
        this.runOnStartup = runOnStartup;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        if (!runOnStartup) {
            return;
        }
        log.info("기동 시 지난주 주간 통계 집계");
        runLifeHabitWeeklyStat();
        runStudyTopicWeeklyStat();
        runHealthLogWeeklyStat();
        runAssignmentWeeklyStat();
        runMealWeeklyStat();
    }

    @Scheduled(cron = CRON)
    public void runLifeHabitWeeklyStat() {
        execute(HabitWeeklyStatJobConfig.JOB_NAME);
    }

    @Scheduled(cron = CRON)
    public void runStudyTopicWeeklyStat() {
        execute(StudyTopicWeeklyStatJobConfig.JOB_NAME);
    }

    @Scheduled(cron = CRON)
    public void runHealthLogWeeklyStat() {
        execute(HealthLogWeeklyStatJobConfig.JOB_NAME);
    }

    @Scheduled(cron = CRON)
    public void runAssignmentWeeklyStat() {
        execute(AssignmentWeeklyStatJobConfig.JOB_NAME);
    }

    @Scheduled(cron = CRON)
    public void runMealWeeklyStat() {
        execute(MealWeeklyStatJobConfig.JOB_NAME);
    }

    // Spring Batch는 Job이 실패해도 예외를 던지지 않고 FAILED 상태의 JobExecution만 돌려준다 —
    // 상태를 검사하지 않으면 실패가 로그에도 남지 않는다. Job마다 별도 스케줄 메서드라 하나가 실패해도 나머지는 계속 돈다.
    private void execute(String jobName) {
        try {
            JobExecution execution = runner.run(jobName, null);
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                log.info("{} 완료 (executionId={})", jobName, execution.getId());
            } else {
                log.error("{} 실패: status={}, exitStatus={}, executionId={}, 원인={}", jobName, execution.getStatus(),
                        execution.getExitStatus().getExitCode(), execution.getId(),
                        execution.getAllFailureExceptions());
            }
        } catch (Exception e) {
            log.error("{} 스케줄 실행 실패", jobName, e);
        }
    }
}
