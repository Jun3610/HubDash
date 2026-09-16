package com.junyoung.dashboard.domain.schedule.dto;

import com.junyoung.dashboard.domain.schedule.entity.Event;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EventResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        Event event = new Event("발표 준비", LocalDateTime.of(2026, 9, 16, 10, 0),
                LocalDateTime.of(2026, 9, 16, 11, 0), "회의실 A", "슬라이드 점검", false);

        EventResponse response = EventResponse.from(event);

        assertThat(response.title()).isEqualTo("발표 준비");
        assertThat(response.startAt()).isEqualTo(LocalDateTime.of(2026, 9, 16, 10, 0));
        assertThat(response.endAt()).isEqualTo(LocalDateTime.of(2026, 9, 16, 11, 0));
        assertThat(response.location()).isEqualTo("회의실 A");
        assertThat(response.description()).isEqualTo("슬라이드 점검");
        assertThat(response.allDay()).isFalse();
    }
}
