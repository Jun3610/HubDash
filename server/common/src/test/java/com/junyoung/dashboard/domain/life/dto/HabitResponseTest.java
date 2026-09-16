package com.junyoung.dashboard.domain.life.dto;

import com.junyoung.dashboard.domain.life.entity.Habit;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HabitResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        Habit habit = new Habit("아침 스트레칭", "매일 아침 10분");

        HabitResponse response = HabitResponse.from(habit);

        assertThat(response.name()).isEqualTo("아침 스트레칭");
        assertThat(response.description()).isEqualTo("매일 아침 10분");
    }
}
