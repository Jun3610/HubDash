package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock
    private HabitRepository habitRepository;

    private HabitService habitService;

    @BeforeEach
    void setUp() {
        habitService = new HabitService(habitRepository);
    }

    @Test
    void createsHabitUsingRequestFields() {
        HabitRequest request = new HabitRequest("아침 스트레칭", "매일 아침 10분");
        when(habitRepository.save(any(Habit.class)))
                .thenReturn(new Habit("아침 스트레칭", "매일 아침 10분"));

        HabitResponse response = habitService.create(request);

        assertThat(response.name()).isEqualTo("아침 스트레칭");

        ArgumentCaptor<Habit> captor = ArgumentCaptor.forClass(Habit.class);
        verify(habitRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(request.name());
        assertThat(captor.getValue().getDescription()).isEqualTo(request.description());
    }

    @Test
    void throwsWhenHabitNotFound() {
        when(habitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
