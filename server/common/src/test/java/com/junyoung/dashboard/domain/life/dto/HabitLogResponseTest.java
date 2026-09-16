package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class HabitLogResponseTest {

    @Test
    void mapsEntityFieldsIncludingHabitId() {
        Habit habit = new Habit("아침 스트레칭", null);
        ReflectionTestUtils.setField(habit, "id", 1L);
        HabitLog log = new HabitLog(habit, LocalDate.of(2026, 9, 16), true, "완료");

        HabitLogResponse response = HabitLogResponse.from(log);

        assertThat(response.completed()).isTrue();
        assertThat(response.notes()).isEqualTo("완료");
        assertThat(response.habitId()).isEqualTo(1L);
    }
}
