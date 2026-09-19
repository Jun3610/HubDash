package com.junyoung.dashboard.analytics.study;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.domain.study.entity.StudyTopicWeeklyStat;
import com.junyoung.dashboard.domain.study.repository.StudyProgressRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicRepository;
import com.junyoung.dashboard.domain.study.repository.StudyTopicWeeklyStatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// study(StudyProgress) 주간 학습 시간 집계 — life(HabitLog) 파일럿(이슈 #54)과 같은 방식으로 실제 JobOperator로 검증한다.
// 컨텍스트에 Job 빈이 여러 개 존재하므로 이 테스트가 검증할 Job을 명시적으로 지정한다.
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class StudyTopicWeeklyStatJobIntegrationTest {

    @Autowired
    private JobOperatorTestUtils jobOperatorTestUtils;

    @Autowired
    @Qualifier(StudyTopicWeeklyStatJobConfig.JOB_NAME)
    private Job studyTopicWeeklyStatJob;

    @BeforeEach
    void setJob() {
        jobOperatorTestUtils.setJob(studyTopicWeeklyStatJob);
    }

    @Autowired
    private StudyTopicRepository studyTopicRepository;

    @Autowired
    private StudyProgressRepository studyProgressRepository;

    @Autowired
    private StudyTopicWeeklyStatRepository studyTopicWeeklyStatRepository;

    @Test
    void aggregatesSessionsWithinTargetWeekIntoWeeklyStat() throws Exception {
        StudyTopic topic = studyTopicRepository.save(new StudyTopic("자료구조", null));
        LocalDate weekStart = LocalDate.of(2026, 9, 14); // 월요일

        studyProgressRepository.save(new StudyProgress(topic, weekStart, 30, null));
        studyProgressRepository.save(new StudyProgress(topic, weekStart.plusDays(2), 45, null));
        studyProgressRepository.save(new StudyProgress(topic, weekStart.plusDays(6), 20, null));
        // 대상 주 밖 — 집계에 포함되면 안 됨.
        studyProgressRepository.save(new StudyProgress(topic, weekStart.minusDays(1), 100, null));
        studyProgressRepository.save(new StudyProgress(topic, weekStart.plusDays(7), 100, null));

        JobExecution execution = jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        StudyTopicWeeklyStat stat = studyTopicWeeklyStatRepository.findByTopicIdAndWeekStart(topic.getId(), weekStart)
                .orElseThrow();
        assertThat(stat.getSessionCount()).isEqualTo(3);
        assertThat(stat.getTotalMinutes()).isEqualTo(95);
    }

    @Test
    void rerunningSameWeekUpdatesExistingStatInsteadOfCreatingDuplicate() throws Exception {
        StudyTopic topic = studyTopicRepository.save(new StudyTopic("알고리즘", null));
        LocalDate weekStart = LocalDate.of(2026, 8, 3);

        studyProgressRepository.save(new StudyProgress(topic, weekStart, 40, null));
        jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        StudyTopicWeeklyStat firstRun = studyTopicWeeklyStatRepository.findByTopicIdAndWeekStart(topic.getId(), weekStart)
                .orElseThrow();
        Long statId = firstRun.getId();

        studyProgressRepository.save(new StudyProgress(topic, weekStart.plusDays(1), 10, null));
        jobOperatorTestUtils.startJob(weekParams(weekStart, 2L));

        StudyTopicWeeklyStat secondRun = studyTopicWeeklyStatRepository.findByTopicIdAndWeekStart(topic.getId(), weekStart)
                .orElseThrow();
        assertThat(secondRun.getId()).isEqualTo(statId);
        assertThat(secondRun.getSessionCount()).isEqualTo(2);
        assertThat(secondRun.getTotalMinutes()).isEqualTo(50);
    }

    @Test
    void topicWithNoSessionsInTargetWeekProducesNoStat() throws Exception {
        StudyTopic topic = studyTopicRepository.save(new StudyTopic("운영체제", null));
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        studyProgressRepository.save(new StudyProgress(topic, weekStart.minusWeeks(1), 30, null));

        jobOperatorTestUtils.startJob(weekParams(weekStart, 1L));

        Optional<StudyTopicWeeklyStat> stat =
                studyTopicWeeklyStatRepository.findByTopicIdAndWeekStart(topic.getId(), weekStart);
        assertThat(stat).isEmpty();
    }

    private JobParameters weekParams(LocalDate weekStart, long runId) {
        return new JobParametersBuilder()
                .addString("weekStart", weekStart.toString())
                .addLong("run.id", runId)
                .toJobParameters();
    }
}
