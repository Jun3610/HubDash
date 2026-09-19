package com.junyoung.dashboard.analytics.study;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.entity.StudyTopicWeeklyStat;
import com.junyoung.dashboard.domain.study.repository.StudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicWeeklyStatRepository;
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

// study(StudyProgress) 주간 학습 시간 집계 — life(HabitLog) 파일럿(이슈 #54)의 패턴을 그대로 재사용.
@Configuration
public class StudyTopicWeeklyStatJobConfig {

    public static final String JOB_NAME = "studyTopicWeeklyStatJob";
    private static final int CHUNK_SIZE = 10;

    private final StudyProgressRepository studyProgressRepository;
    private final StudyTopicRepository studyTopicRepository;
    private final StudyTopicWeeklyStatRepository studyTopicWeeklyStatRepository;

    public StudyTopicWeeklyStatJobConfig(StudyProgressRepository studyProgressRepository,
                                          StudyTopicRepository studyTopicRepository,
                                          StudyTopicWeeklyStatRepository studyTopicWeeklyStatRepository) {
        this.studyProgressRepository = studyProgressRepository;
        this.studyTopicRepository = studyTopicRepository;
        this.studyTopicWeeklyStatRepository = studyTopicWeeklyStatRepository;
    }

    @Bean(JOB_NAME)
    public Job studyTopicWeeklyStatJob(JobRepository jobRepository, Step studyTopicWeeklyStatStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(studyTopicWeeklyStatStep)
                .build();
    }

    @Bean
    public Step studyTopicWeeklyStatStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          ItemReader<Long> topicIdReader,
                                          ItemProcessor<Long, StudyTopicWeeklyStat> studyTopicWeeklyStatProcessor,
                                          ItemWriter<StudyTopicWeeklyStat> studyTopicWeeklyStatWriter) {
        return new StepBuilder("studyTopicWeeklyStatStep", jobRepository)
                .<Long, StudyTopicWeeklyStat>chunk(CHUNK_SIZE)
                .reader(topicIdReader)
                .processor(studyTopicWeeklyStatProcessor)
                .writer(studyTopicWeeklyStatWriter)
                .transactionManager(transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public ItemReader<Long> topicIdReader(@Value("#{jobParameters['weekStart']}") String weekStartParam) {
        LocalDate weekStart = LocalDate.parse(weekStartParam);
        LocalDate weekEnd = weekStart.plusDays(6);
        return new ListItemReader<>(studyProgressRepository.findDistinctTopicIdsWithProgressBetween(weekStart, weekEnd));
    }

    @Bean
    @StepScope
    public ItemProcessor<Long, StudyTopicWeeklyStat> studyTopicWeeklyStatProcessor(
            @Value("#{jobParameters['weekStart']}") String weekStartParam) {
        LocalDate weekStart = LocalDate.parse(weekStartParam);
        LocalDate weekEnd = weekStart.plusDays(6);
        return topicId -> {
            StudyTopic topic = studyTopicRepository.findById(topicId).orElse(null);
            if (topic == null) {
                return null;
            }
            long sessionCount = studyProgressRepository.countByTopicIdAndStudiedAtBetween(topicId, weekStart, weekEnd);
            long totalMinutes = studyProgressRepository.sumMinutesByTopicIdAndStudiedAtBetween(topicId, weekStart, weekEnd);

            return studyTopicWeeklyStatRepository.findByTopicIdAndWeekStart(topicId, weekStart)
                    .map(existing -> {
                        existing.updateCounts((int) sessionCount, (int) totalMinutes);
                        return existing;
                    })
                    .orElseGet(() -> new StudyTopicWeeklyStat(topic, weekStart, (int) sessionCount, (int) totalMinutes));
        };
    }

    @Bean
    public ItemWriter<StudyTopicWeeklyStat> studyTopicWeeklyStatWriter() {
        return chunk -> studyTopicWeeklyStatRepository.saveAll(chunk.getItems());
    }
}
