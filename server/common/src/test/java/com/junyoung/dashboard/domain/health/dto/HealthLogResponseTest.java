package com.junyoung.dashboard.domain.health.dto;

import com.junyoung.dashboard.domain.health.entity.HealthLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HealthLogResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        HealthLog log = new HealthLog(LocalDateTime.of(2026, 9, 1, 7, 30), 70.5, 7.5, "가벼운 하루");

        HealthLogResponse response = HealthLogResponse.from(log);

        assertThat(response.recordedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 7, 30));
        assertThat(response.weightKg()).isEqualTo(70.5);
        assertThat(response.sleepHours()).isEqualTo(7.5);
        assertThat(response.notes()).isEqualTo("가벼운 하루");
    }
}
