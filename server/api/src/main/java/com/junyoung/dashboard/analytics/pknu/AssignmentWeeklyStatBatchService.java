package com.junyoung.dashboard.analytics.pknu;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class AssignmentWeeklyStatBatchService {

    private final JobOperator jobOperator;
    private final Job assignmentWeeklyStatJob;

    public AssignmentWeeklyStatBatchService(JobOperator jobOperator,
                                            @Qualifier(AssignmentWeeklyStatJobConfig.JOB_NAME) Job assignmentWeeklyStatJob) {
        this.jobOperator = jobOperator;
        this.assignmentWeeklyStatJob = assignmentWeeklyStatJob;
    }

    public JobExecution run(LocalDate weekStart) throws Exception {
        LocalDate targetWeekStart = weekStart != null ? weekStart : previousWeekMonday();
        JobParameters params = new JobParametersBuilder()
                .addString("weekStart", targetWeekStart.toString())
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
        return jobOperator.start(assignmentWeeklyStatJob, params);
    }

    private LocalDate previousWeekMonday() {
        LocalDate thisMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return thisMonday.minusWeeks(1);
    }
}
