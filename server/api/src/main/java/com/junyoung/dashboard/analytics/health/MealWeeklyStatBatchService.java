package com.junyoung.dashboard.analytics.health;

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
public class MealWeeklyStatBatchService {

    private final JobLauncher jobLauncher;
    private final Job mealWeeklyStatJob;

    public MealWeeklyStatBatchService(JobLauncher jobLauncher,
                                       @Qualifier(MealWeeklyStatJobConfig.JOB_NAME) Job mealWeeklyStatJob) {
        this.jobLauncher = jobLauncher;
        this.mealWeeklyStatJob = mealWeeklyStatJob;
    }

    public JobExecution run(LocalDate weekStart) throws Exception {
        LocalDate targetWeekStart = weekStart != null ? weekStart : previousWeekMonday();
        JobParameters params = new JobParametersBuilder()
                .addString("weekStart", targetWeekStart.toString())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        return jobLauncher.run(mealWeeklyStatJob, params);
    }

    private LocalDate previousWeekMonday() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusWeeks(1);
    }
}
