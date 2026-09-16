package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.WorkoutLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class WorkoutLogResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        WorkoutLog log = new WorkoutLog(LocalDate.of(2026, 9, 1), "러닝", 30, 300, "상쾌함");

        WorkoutLogResponse response = WorkoutLogResponse.from(log);

        assertThat(response.type()).isEqualTo("러닝");
        assertThat(response.durationMinutes()).isEqualTo(30);
        assertThat(response.caloriesBurned()).isEqualTo(300);
    }
}
