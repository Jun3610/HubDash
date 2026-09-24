package com.junyoung.dashboard.domain.health.consumer;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawHealthLogRecordedAtParseTest {

    @Test
    void dateOnlyBecomesMidnight() {
        assertThat(RawHealthLogConsumer.parseRecordedAt("2026-09-17"))
                .isEqualTo(LocalDateTime.of(2026, 9, 17, 0, 0));
    }

    @Test
    void keepsTimeWhenGiven() {
        assertThat(RawHealthLogConsumer.parseRecordedAt(" 2026-09-17T07:30 "))
                .isEqualTo(LocalDateTime.of(2026, 9, 17, 7, 30));
        assertThat(RawHealthLogConsumer.parseRecordedAt("2026-09-17T07:30:15"))
                .isEqualTo(LocalDateTime.of(2026, 9, 17, 7, 30, 15));
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> RawHealthLogConsumer.parseRecordedAt("이건-날짜-아님"))
                .isInstanceOf(DateTimeParseException.class);
    }
}
