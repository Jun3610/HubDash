package com.junyoung.dashboard.analytics.pknu;

import com.junyoung.dashboard.domain.pknu.entity.AssignmentWeeklyStat;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentRepository;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentWeeklyStatRepository;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
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

// pknu(Assignment) 주간 과제 완료율 집계 — life 파일럿(이슈 #54)의 패턴을 그대로 재사용.
// 집계 기준은 마감일(dueDate)이며, 대상 주에 마감인 과제가 있는 코스만 처리한다.
@Configuration
public class AssignmentWeeklyStatJobConfig {

    public static final String JOB_NAME = "assignmentWeeklyStatJob";
    private static final int CHUNK_SIZE = 10;

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final AssignmentWeeklyStatRepository assignmentWeeklyStatRepository;

    public AssignmentWeeklyStatJobConfig(AssignmentRepository assignmentRepository,
                                          CourseRepository courseRepository,
                                          AssignmentWeeklyStatRepository assignmentWeeklyStatRepository) {
        this.assignmentRepository = assignmentRepository;
        this.courseRepository = courseRepository;
        this.assignmentWeeklyStatRepository = assignmentWeeklyStatRepository;
    }

    @Bean(JOB_NAME)
    public Job assignmentWeeklyStatJob(JobRepository jobRepository, Step assignmentWeeklyStatStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(assignmentWeeklyStatStep)
                .build();
    }

    @Bean
    public Step assignmentWeeklyStatStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          ItemReader<Long> courseIdReader,
                                          ItemProcessor<Long, AssignmentWeeklyStat> assignmentWeeklyStatProcessor,
                                          ItemWriter<AssignmentWeeklyStat> assignmentWeeklyStatWriter) {
        return new StepBuilder("assignmentWeeklyStatStep", jobRepository)
                .<Long, AssignmentWeeklyStat>chunk(CHUNK_SIZE, transactionManager)
                .reader(courseIdReader)
                .processor(assignmentWeeklyStatProcessor)
                .writer(assignmentWeeklyStatWriter)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Long> courseIdReader(@Value("#{jobParameters['weekStart']}") String weekStartParam) {
        LocalDate weekStart = LocalDate.parse(weekStartParam);
        LocalDate weekEnd = weekStart.plusDays(6);
        return new ListItemReader<>(assignmentRepository.findDistinctCourseIdsWithDueBetween(weekStart, weekEnd));
    }

    @Bean
    @StepScope
    public ItemProcessor<Long, AssignmentWeeklyStat> assignmentWeeklyStatProcessor(
            @Value("#{jobParameters['weekStart']}") String weekStartParam) {
        LocalDate weekStart = LocalDate.parse(weekStartParam);
        LocalDate weekEnd = weekStart.plusDays(6);
        return courseId -> {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                return null;
            }
            long totalCount = assignmentRepository.countByCourseIdAndDueDateBetween(courseId, weekStart, weekEnd);
            long completedCount = assignmentRepository
                    .countByCourseIdAndDueDateBetweenAndCompletedTrue(courseId, weekStart, weekEnd);

            return assignmentWeeklyStatRepository.findByCourseIdAndWeekStart(courseId, weekStart)
                    .map(existing -> {
                        existing.updateCounts((int) totalCount, (int) completedCount);
                        return existing;
                    })
                    .orElseGet(() -> new AssignmentWeeklyStat(course, weekStart, (int) totalCount, (int) completedCount));
        };
    }

    @Bean
    public ItemWriter<AssignmentWeeklyStat> assignmentWeeklyStatWriter() {
        return chunk -> assignmentWeeklyStatRepository.saveAll(chunk.getItems());
    }
}
