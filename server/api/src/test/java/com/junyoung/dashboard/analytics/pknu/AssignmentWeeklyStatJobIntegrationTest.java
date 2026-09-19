package com.junyoung.dashboard.analytics.pknu;

import com.junyoung.dashboard.domain.pknu.entity.Assignment;
import com.junyoung.dashboard.domain.pknu.entity.AssignmentWeeklyStat;
import com.junyoung.dashboard.domain.pknu.entity.Course;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentRepository;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentWeeklyStatRepository;
import com.junyoung.dashboard.domain.pknu.repository.CourseRepository;
import com.junyoung.dashboard.domain.pknu.repository.SemesterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// pknu(Assignment) 주간 과제 완료율 집계 — life 파일럿(이슈 #54)과 같은 방식으로 실제 JobLauncher로 검증한다.
// 컨텍스트에 Job 빈이 여러 개 존재하므로 이 테스트가 검증할 Job을 명시적으로 지정한다.
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
class AssignmentWeeklyStatJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier(AssignmentWeeklyStatJobConfig.JOB_NAME)
    private Job assignmentWeeklyStatJob;

    @BeforeEach
    void setJob() {
        jobLauncherTestUtils.setJob(assignmentWeeklyStatJob);
    }

    @Autowired
    private SemesterRepository semesterRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentWeeklyStatRepository assignmentWeeklyStatRepository;

    @Test
    void countsTotalAndCompletedAssignmentsDueWithinTargetWeek() throws Exception {
        Course course = newCourse("자료구조");
        LocalDate weekStart = LocalDate.of(2026, 9, 14); // 월요일

        assignmentRepository.save(new Assignment(course, "과제1", weekStart, true, null));
        assignmentRepository.save(new Assignment(course, "과제2", weekStart.plusDays(3), false, null));
        assignmentRepository.save(new Assignment(course, "과제3", weekStart.plusDays(6), true, null));
        // 마감이 대상 주 밖 — 집계에 포함되면 안 됨.
        assignmentRepository.save(new Assignment(course, "지난주", weekStart.minusDays(1), true, null));
        assignmentRepository.save(new Assignment(course, "다음주", weekStart.plusDays(7), true, null));

        JobExecution execution = jobLauncherTestUtils.launchJob(weekParams(weekStart, 1L));

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        AssignmentWeeklyStat stat = assignmentWeeklyStatRepository
                .findByCourseIdAndWeekStart(course.getId(), weekStart).orElseThrow();
        assertThat(stat.getTotalCount()).isEqualTo(3);
        assertThat(stat.getCompletedCount()).isEqualTo(2);
    }

    @Test
    void rerunningSameWeekUpdatesExistingStatInsteadOfCreatingDuplicate() throws Exception {
        Course course = newCourse("알고리즘");
        LocalDate weekStart = LocalDate.of(2026, 8, 3);

        Assignment first = assignmentRepository.save(new Assignment(course, "과제1", weekStart, false, null));
        jobLauncherTestUtils.launchJob(weekParams(weekStart, 1L));

        AssignmentWeeklyStat firstRun = assignmentWeeklyStatRepository
                .findByCourseIdAndWeekStart(course.getId(), weekStart).orElseThrow();
        Long statId = firstRun.getId();
        assertThat(firstRun.getCompletedCount()).isZero();

        first.update(course, first.getTitle(), first.getDueDate(), true, null);
        assignmentRepository.save(first);
        assignmentRepository.save(new Assignment(course, "과제2", weekStart.plusDays(1), false, null));
        jobLauncherTestUtils.launchJob(weekParams(weekStart, 2L));

        AssignmentWeeklyStat secondRun = assignmentWeeklyStatRepository
                .findByCourseIdAndWeekStart(course.getId(), weekStart).orElseThrow();
        assertThat(secondRun.getId()).isEqualTo(statId);
        assertThat(secondRun.getTotalCount()).isEqualTo(2);
        assertThat(secondRun.getCompletedCount()).isEqualTo(1);
    }

    @Test
    void courseWithNoAssignmentsDueInTargetWeekProducesNoStat() throws Exception {
        Course course = newCourse("운영체제");
        LocalDate weekStart = LocalDate.of(2026, 7, 6);
        assignmentRepository.save(new Assignment(course, "지난주 마감", weekStart.minusWeeks(1), true, null));

        jobLauncherTestUtils.launchJob(weekParams(weekStart, 1L));

        Optional<AssignmentWeeklyStat> stat =
                assignmentWeeklyStatRepository.findByCourseIdAndWeekStart(course.getId(), weekStart);
        assertThat(stat).isEmpty();
    }

    private Course newCourse(String name) {
        Semester semester = semesterRepository.save(
                new Semester("2026-2학기", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 20)));
        return courseRepository.save(new Course(semester, name, "김교수", 3));
    }

    private JobParameters weekParams(LocalDate weekStart, long runId) {
        return new JobParametersBuilder()
                .addString("weekStart", weekStart.toString())
                .addLong("run.id", runId)
                .toJobParameters();
    }
}
