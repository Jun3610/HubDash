package com.junyoung.dashboard.domain.reminder.dto;

import com.junyoung.dashboard.domain.reminder.entity.Reminder;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReminderResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        Reminder reminder = new Reminder(
                "과제 마감 임박", LocalDateTime.of(2026, 9, 20, 9, 0), "pknu", 42L, false);

        ReminderResponse response = ReminderResponse.from(reminder);

        assertThat(response.title()).isEqualTo("과제 마감 임박");
        assertThat(response.targetAt()).isEqualTo(LocalDateTime.of(2026, 9, 20, 9, 0));
        assertThat(response.targetDomain()).isEqualTo("pknu");
        assertThat(response.targetEntityId()).isEqualTo(42L);
        assertThat(response.sent()).isFalse();
    }
}
