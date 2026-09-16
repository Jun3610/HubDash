package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.WorkoutLogRequest;
import com.junyoung.dashboard.domain.health.dto.WorkoutLogResponse;
import com.junyoung.dashboard.domain.health.entity.WorkoutLog;
import com.junyoung.dashboard.domain.health.repository.WorkoutLogRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkoutLogServiceTest {

    @Mock
    private WorkoutLogRepository workoutLogRepository;

    private WorkoutLogService workoutLogService;

    @BeforeEach
    void setUp() {
        workoutLogService = new WorkoutLogService(workoutLogRepository);
    }

    @Test
    void createsWorkoutLogUsingRequestFields() {
        WorkoutLogRequest request = new WorkoutLogRequest(LocalDate.of(2026, 9, 1), "러닝", 30, 300, null);
        when(workoutLogRepository.save(any(WorkoutLog.class)))
                .thenReturn(new WorkoutLog(LocalDate.of(2026, 9, 1), "러닝", 30, 300, null));

        WorkoutLogResponse response = workoutLogService.create(request);

        assertThat(response.type()).isEqualTo("러닝");

        ArgumentCaptor<WorkoutLog> captor = ArgumentCaptor.forClass(WorkoutLog.class);
        verify(workoutLogRepository).save(captor.capture());
        assertThat(captor.getValue().getDurationMinutes()).isEqualTo(request.durationMinutes());
        assertThat(captor.getValue().getType()).isEqualTo(request.type());
    }

    @Test
    void throwsWhenWorkoutLogNotFound() {
        when(workoutLogRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> workoutLogService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updatesEveryFieldWithoutTransposingParameters() {
        WorkoutLog existing = new WorkoutLog(LocalDate.of(2026, 1, 1), "걷기", 20, 100, "기존");
        when(workoutLogRepository.findById(1L)).thenReturn(Optional.of(existing));

        WorkoutLogRequest request = new WorkoutLogRequest(LocalDate.of(2026, 2, 2), "수영", 45, 400, "새 기록");

        WorkoutLogResponse response = workoutLogService.update(1L, request);

        assertThat(response.type()).isEqualTo("수영");
        assertThat(response.durationMinutes()).isEqualTo(45);
        assertThat(response.caloriesBurned()).isEqualTo(400);
        assertThat(response.notes()).isEqualTo("새 기록");
    }
}
