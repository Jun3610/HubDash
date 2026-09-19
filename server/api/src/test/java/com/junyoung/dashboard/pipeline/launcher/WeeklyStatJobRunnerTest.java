package com.junyoung.dashboard.pipeline.launcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyStatJobRunnerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job lifeJob;

    @Mock
    private Job mealJob;

    private WeeklyStatJobRunner runnerAt(String date) {
        Clock clock = Clock.fixed(Instant.parse(date + "T03:00:00Z"), ZONE);
        return new WeeklyStatJobRunner(jobOperator, Map.of("lifeJob", lifeJob, "mealJob", mealJob), clock);
    }

    private JobParameters startedWith(Job job) throws Exception {
        ArgumentCaptor<JobParameters> captor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobOperator).start(org.mockito.ArgumentMatchers.eq(job), captor.capture());
        return captor.getValue();
    }

    @Test
    void runsTheJobFoundByNameWithTheGivenWeek() throws Exception {
        JobExecution expected = org.mockito.Mockito.mock(JobExecution.class);
        when(jobOperator.start(any(Job.class), any(JobParameters.class))).thenReturn(expected);

        JobExecution actual = runnerAt("2026-09-22").run("mealJob", LocalDate.of(2026, 8, 3));

        assertThat(actual).isSameAs(expected);
        JobParameters params = startedWith(mealJob);
        assertThat(params.getString("weekStart")).isEqualTo("2026-08-03");
        assertThat(params.getLong("run.id")).isNotNull();
        verify(jobOperator, never()).start(org.mockito.ArgumentMatchers.eq(lifeJob), any(JobParameters.class));
    }

    @Test
    void unknownJobNameFailsWithoutStartingAnything() {
        assertThatThrownBy(() -> runnerAt("2026-09-22").run("noSuchJob", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("noSuchJob");
        org.mockito.Mockito.verifyNoInteractions(jobOperator);
    }

    @Test
    void defaultsToTheMondayOfTheLastCompletedWeek() throws Exception {
        // 2026-09-14는 월요일. 어느 요일에 실행하든 "방금 끝난 주"인 09-07이 아니라, 진행 중인 주 기준 지난주여야 한다.
        String[][] cases = {
                {"2026-09-21", "2026-09-14"}, // 월요일: 방금 끝난 주(09-14~09-20)
                {"2026-09-22", "2026-09-14"}, // 화요일: 같은 주를 재집계
                {"2026-09-27", "2026-09-14"}, // 일요일(다음 주의 끝): 아직 09-21주가 진행 중이므로 지난주는 09-14
                {"2026-09-20", "2026-09-07"}, // 일요일(09-14주의 마지막 날): 그 주는 아직 안 끝났으므로 09-07
        };
        for (String[] c : cases) {
            assertThat(runnerAt(c[0]).previousWeekMonday())
                    .as("실행일 %s", c[0]).isEqualTo(LocalDate.parse(c[1]));
        }
    }

    @Test
    void nullWeekStartPassesThePreviousMondayAsParameter() throws Exception {
        when(jobOperator.start(any(Job.class), any(JobParameters.class)))
                .thenReturn(org.mockito.Mockito.mock(JobExecution.class));

        runnerAt("2026-09-23").run("lifeJob", null);

        assertThat(startedWith(lifeJob).getString("weekStart")).isEqualTo("2026-09-14");
    }
}
