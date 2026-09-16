package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.domain.life.repository.HabitLogRepository;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
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
class HabitLogServiceTest {

    @Mock
    private HabitLogRepository habitLogRepository;

    @Mock
    private HabitRepository habitRepository;

    private HabitLogService habitLogService;

    @BeforeEach
    void setUp() {
        habitLogService = new HabitLogService(habitLogRepository, habitRepository);
    }

    @Test
    void createsLogUnderExistingHabit() {
        Habit habit = new Habit("아침 스트레칭", null);
        HabitLogRequest request = new HabitLogRequest(1L, LocalDate.of(2026, 9, 16), true, "완료");
        when(habitRepository.findById(1L)).thenReturn(Optional.of(habit));
        when(habitLogRepository.save(any(HabitLog.class)))
                .thenReturn(new HabitLog(habit, LocalDate.of(2026, 9, 16), true, "완료"));

        HabitLogResponse response = habitLogService.create(request);

        assertThat(response.completed()).isTrue();

        ArgumentCaptor<HabitLog> captor = ArgumentCaptor.forClass(HabitLog.class);
        verify(habitLogRepository).save(captor.capture());
        assertThat(captor.getValue().getHabit()).isSameAs(habit);
    }

    @Test
    void throwsWhenHabitMissingOnCreate() {
        HabitLogRequest request = new HabitLogRequest(1L, LocalDate.of(2026, 9, 16), true, null);
        when(habitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitLogService.create(request))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void movesLogToNewHabitOnUpdate() {
        Habit oldHabit = new Habit("아침 스트레칭", null);
        Habit newHabit = new Habit("저녁 독서", null);
        HabitLog log = new HabitLog(oldHabit, LocalDate.of(2026, 9, 16), true, null);
        HabitLogRequest request = new HabitLogRequest(2L, LocalDate.of(2026, 9, 17), false, "이동됨");
        when(habitLogRepository.findById(10L)).thenReturn(Optional.of(log));
        when(habitRepository.findById(2L)).thenReturn(Optional.of(newHabit));

        habitLogService.update(10L, request);

        assertThat(log.getHabit()).isSameAs(newHabit);
        assertThat(log.getHabit()).isNotSameAs(oldHabit);
        assertThat(log.getCompleted()).isFalse();
    }

    @Test
    void throwsWhenNewHabitMissingOnUpdate() {
        Habit oldHabit = new Habit("아침 스트레칭", null);
        HabitLog log = new HabitLog(oldHabit, LocalDate.of(2026, 9, 16), true, null);
        HabitLogRequest request = new HabitLogRequest(2L, LocalDate.of(2026, 9, 17), false, null);
        when(habitLogRepository.findById(10L)).thenReturn(Optional.of(log));
        when(habitRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitLogService.update(10L, request))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
