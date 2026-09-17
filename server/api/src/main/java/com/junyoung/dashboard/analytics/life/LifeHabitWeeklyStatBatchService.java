package com.junyoung.dashboard.analytics.life;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class LifeHabitWeeklyStatBatchService {

    private final JobLauncher jobLauncher;
    private final Job lifeHabitWeeklyStatJob;

    public LifeHabitWeeklyStatBatchService(JobLauncher jobLauncher,
                                            @Qualifier(HabitWeeklyStatJobConfig.JOB_NAME) Job lifeHabitWeeklyStatJob) {
        this.jobLauncher = jobLauncher;
        this.lifeHabitWeeklyStatJob = lifeHabitWeeklyStatJob;
    }

    public JobExecution run(LocalDate weekStart) throws Exception {
        LocalDate targetWeekStart = weekStart != null ? weekStart : previousWeekMonday();
        JobParameters params = new JobParametersBuilder()
                .addString("weekStart", targetWeekStart.toString())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        return jobLauncher.run(lifeHabitWeeklyStatJob, params);
    }

    // 스케줄러가 "방금 끝난 주"를 집계하도록 기본값을 지난주 월요일로 둔다.
    private LocalDate previousWeekMonday() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusWeeks(1);
    }
}
