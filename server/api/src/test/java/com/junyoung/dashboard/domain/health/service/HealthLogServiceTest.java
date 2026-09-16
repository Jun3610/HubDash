package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.HealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.HealthLogResponse;
import com.junyoung.dashboard.domain.health.entity.HealthLog;
import com.junyoung.dashboard.domain.health.repository.HealthLogRepository;
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
class HealthLogServiceTest {

    @Mock
    private HealthLogRepository healthLogRepository;

    private HealthLogService healthLogService;

    @BeforeEach
    void setUp() {
        healthLogService = new HealthLogService(healthLogRepository);
    }

    @Test
    void createsHealthLogUsingRequestFields() {
        HealthLogRequest request = new HealthLogRequest(LocalDate.of(2026, 9, 1), 70.5, 7.5, null);
        when(healthLogRepository.save(any(HealthLog.class)))
                .thenReturn(new HealthLog(LocalDate.of(2026, 9, 1), 70.5, 7.5, null));

        HealthLogResponse response = healthLogService.create(request);

        assertThat(response.weightKg()).isEqualTo(70.5);

        ArgumentCaptor<HealthLog> captor = ArgumentCaptor.forClass(HealthLog.class);
        verify(healthLogRepository).save(captor.capture());
        assertThat(captor.getValue().getRecordedAt()).isEqualTo(request.recordedAt());
        assertThat(captor.getValue().getSleepHours()).isEqualTo(request.sleepHours());
    }

    @Test
    void throwsWhenHealthLogNotFound() {
        when(healthLogRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthLogService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updatesEveryFieldWithoutTransposingParameters() {
        HealthLog existing = new HealthLog(LocalDate.of(2026, 1, 1), 68.0, 6.0, "기존 메모");
        when(healthLogRepository.findById(1L)).thenReturn(Optional.of(existing));

        HealthLogRequest request = new HealthLogRequest(LocalDate.of(2026, 2, 2), 71.0, 8.0, "새 메모");

        HealthLogResponse response = healthLogService.update(1L, request);

        assertThat(response.recordedAt()).isEqualTo(LocalDate.of(2026, 2, 2));
        assertThat(response.weightKg()).isEqualTo(71.0);
        assertThat(response.sleepHours()).isEqualTo(8.0);
        assertThat(response.notes()).isEqualTo("새 메모");
    }
}
