package com.junyoung.dashboard.pipeline.launcher;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;

// 주간 집계 Job(life/study/health/pknu/meal)을 이름으로 찾아 대상 주(weekStart)를 파라미터로 실행한다.
// 도메인마다 복제되어 있던 *BatchService를 하나로 합친 것 — 도메인별로 다른 건 Reader/Processor(JobConfig)뿐이다.
@Service
public class WeeklyStatJobRunner {

    private final JobOperator jobOperator;
    private final Map<String, Job> jobsByName;
    private final Clock clock;

    // 스프링이 모든 Job 빈을 빈 이름(=JOB_NAME)을 키로 주입한다.
    // 생성자가 둘이라 스프링이 사용할 생성자를 @Autowired로 명시해야 한다(이슈 #56에서 겪은 문제).
    @Autowired
    public WeeklyStatJobRunner(JobOperator jobOperator, Map<String, Job> jobsByName) {
        this(jobOperator, jobsByName, Clock.systemDefaultZone());
    }

    WeeklyStatJobRunner(JobOperator jobOperator, Map<String, Job> jobsByName, Clock clock) {
        this.jobOperator = jobOperator;
        this.jobsByName = jobsByName;
        this.clock = clock;
    }

    // weekStart가 null이면 "방금 끝난 주(지난주 월요일)"를 집계한다.
    public JobExecution run(String jobName, LocalDate weekStart) throws Exception {
        Job job = jobsByName.get(jobName);
        if (job == null) {
            throw new IllegalArgumentException("등록되지 않은 배치 Job: " + jobName);
        }
        LocalDate targetWeekStart = weekStart != null ? weekStart : previousWeekMonday();
        JobParameters params = new JobParametersBuilder()
                .addString("weekStart", targetWeekStart.toString())
                // 같은 주를 여러 번 재집계할 수 있도록 실행마다 다른 파라미터를 준다.
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        return jobOperator.start(job, params);
    }

    LocalDate previousWeekMonday() {
        LocalDate thisMonday = LocalDate.now(clock).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusWeeks(1);
    }
}
