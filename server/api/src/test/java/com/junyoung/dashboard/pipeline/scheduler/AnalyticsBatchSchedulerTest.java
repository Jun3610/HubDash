package com.junyoung.dashboard.pipeline.scheduler;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.junyoung.dashboard.analytics.health.HealthLogWeeklyStatJobConfig;
import com.junyoung.dashboard.analytics.health.MealWeeklyStatJobConfig;
import com.junyoung.dashboard.analytics.life.HabitWeeklyStatJobConfig;
import com.junyoung.dashboard.analytics.pknu.AssignmentWeeklyStatJobConfig;
import com.junyoung.dashboard.analytics.study.StudyTopicWeeklyStatJobConfig;
import com.junyoung.dashboard.pipeline.launcher.WeeklyStatJobRunner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsBatchSchedulerTest {

    @Mock
    private WeeklyStatJobRunner runner;

    private AnalyticsBatchScheduler scheduler;
    private ListAppender<ILoggingEvent> logs;
    private Logger schedulerLogger;

    @BeforeEach
    void setUp() {
        scheduler = new AnalyticsBatchScheduler(runner);
        schedulerLogger = (Logger) LoggerFactory.getLogger(AnalyticsBatchScheduler.class);
        logs = new ListAppender<>();
        logs.start();
        schedulerLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        schedulerLogger.detachAppender(logs);
    }

    private JobExecution execution(BatchStatus status, ExitStatus exitStatus, List<Throwable> failures) {
        JobExecution execution = mock(JobExecution.class);
        when(execution.getStatus()).thenReturn(status);
        when(execution.getId()).thenReturn(7L);
        if (status != BatchStatus.COMPLETED) {
            when(execution.getExitStatus()).thenReturn(exitStatus);
            when(execution.getAllFailureExceptions()).thenReturn(failures);
        }
        return execution;
    }

    @Test
    void eachScheduledMethodRunsItsOwnJobForTheLastCompletedWeek() throws Exception {
        JobExecution completed = execution(BatchStatus.COMPLETED, ExitStatus.COMPLETED, List.of());
        when(runner.run(any(), isNull())).thenReturn(completed);

        scheduler.runLifeHabitWeeklyStat();
        scheduler.runStudyTopicWeeklyStat();
        scheduler.runHealthLogWeeklyStat();
        scheduler.runAssignmentWeeklyStat();
        scheduler.runMealWeeklyStat();

        // weekStart=null이면 러너가 "지난주 월요일"을 계산한다.
        verify(runner).run(eq(HabitWeeklyStatJobConfig.JOB_NAME), isNull());
        verify(runner).run(eq(StudyTopicWeeklyStatJobConfig.JOB_NAME), isNull());
        verify(runner).run(eq(HealthLogWeeklyStatJobConfig.JOB_NAME), isNull());
        verify(runner).run(eq(AssignmentWeeklyStatJobConfig.JOB_NAME), isNull());
        verify(runner).run(eq(MealWeeklyStatJobConfig.JOB_NAME), isNull());
    }

    @Test
    void completedJobIsLoggedAtInfoWithoutErrors() throws Exception {
        JobExecution completed = execution(BatchStatus.COMPLETED, ExitStatus.COMPLETED, List.of());
        when(runner.run(any(), isNull())).thenReturn(completed);

        scheduler.runMealWeeklyStat();

        assertThat(logs.list).extracting(ILoggingEvent::getLevel).containsOnly(Level.INFO);
        assertThat(logs.list.get(0).getFormattedMessage()).contains(MealWeeklyStatJobConfig.JOB_NAME);
    }

    // Spring Batch는 Job이 실패해도 예외를 던지지 않고 FAILED 상태만 돌려준다 — 예전에는 이 실패가 로그에도 남지 않았다.
    @Test
    void failedJobExecutionIsLoggedAsErrorEvenThoughNoExceptionIsThrown() throws Exception {
        JobExecution failed = execution(BatchStatus.FAILED, ExitStatus.FAILED, List.of(new IllegalStateException("db down")));
        when(runner.run(eq(MealWeeklyStatJobConfig.JOB_NAME), isNull())).thenReturn(failed);

        scheduler.runMealWeeklyStat();

        assertThat(logs.list).hasSize(1);
        ILoggingEvent event = logs.list.get(0);
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage())
                .contains(MealWeeklyStatJobConfig.JOB_NAME).contains("FAILED").contains("db down");
    }

    @Test
    void anyNonCompletedStatusIsTreatedAsAFailure() throws Exception {
        JobExecution stopped = execution(BatchStatus.STOPPED, new ExitStatus("STOPPED"), List.of());
        when(runner.run(any(), isNull())).thenReturn(stopped);

        scheduler.runLifeHabitWeeklyStat();

        assertThat(logs.list.get(0).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void exceptionFromRunnerIsLoggedAndDoesNotPropagate() throws Exception {
        when(runner.run(any(), isNull())).thenThrow(new IllegalStateException("launch failed"));

        scheduler.runStudyTopicWeeklyStat(); // 예외가 밖으로 나오면 안 된다

        assertThat(logs.list).hasSize(1);
        assertThat(logs.list.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logs.list.get(0).getThrowableProxy().getMessage()).isEqualTo("launch failed");
    }

    @Test
    void oneFailingJobDoesNotStopTheOthers() throws Exception {
        when(runner.run(eq(HabitWeeklyStatJobConfig.JOB_NAME), isNull())).thenThrow(new IllegalStateException("boom"));
        JobExecution completed = execution(BatchStatus.COMPLETED, ExitStatus.COMPLETED, List.of());
        when(runner.run(eq(MealWeeklyStatJobConfig.JOB_NAME), isNull())).thenReturn(completed);

        scheduler.runLifeHabitWeeklyStat();
        scheduler.runMealWeeklyStat();

        verify(runner).run(eq(MealWeeklyStatJobConfig.JOB_NAME), isNull());
    }

    @Test
    void allFiveJobsAreScheduledDailyWithAnOverridableCron() {
        List<Method> scheduled = Arrays.stream(AnalyticsBatchScheduler.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Scheduled.class)).toList();

        assertThat(scheduled).hasSize(5);
        for (Method method : scheduled) {
            assertThat(method.getAnnotation(Scheduled.class).cron())
                    .as(method.getName())
                    .isEqualTo("${analytics.batch.cron:0 10 0 * * *}");
        }
    }

    @Test
    void defaultCronFiresEveryDayAtTenPastMidnight() {
        CronExpression cron = CronExpression.parse("0 10 0 * * *");
        LocalDateTime monday = LocalDateTime.of(2026, 9, 21, 0, 10);

        // 월요일 실행이 실패했어도 화요일, 수요일 ... 매일 다시 시도한다.
        assertThat(cron.next(monday)).isEqualTo(LocalDateTime.of(2026, 9, 22, 0, 10));
        assertThat(cron.next(monday.plusDays(1))).isEqualTo(LocalDateTime.of(2026, 9, 23, 0, 10));
    }
}
