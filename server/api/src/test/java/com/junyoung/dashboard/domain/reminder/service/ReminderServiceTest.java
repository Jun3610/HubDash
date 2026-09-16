package com.junyoung.dashboard.domain.reminder.service;

import com.junyoung.dashboard.domain.reminder.dto.ReminderRequest;
import com.junyoung.dashboard.domain.reminder.dto.ReminderResponse;
import com.junyoung.dashboard.domain.reminder.entity.Reminder;
import com.junyoung.dashboard.domain.reminder.repository.ReminderRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private ReminderRepository reminderRepository;

    private ReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new ReminderService(reminderRepository);
    }

    @Test
    void createsReminderUsingRequestFields() {
        ReminderRequest request = new ReminderRequest(
                "과제 마감 임박", LocalDateTime.of(2026, 9, 20, 9, 0), "pknu", 42L, false);
        when(reminderRepository.save(any(Reminder.class)))
                .thenReturn(new Reminder("과제 마감 임박", LocalDateTime.of(2026, 9, 20, 9, 0), "pknu", 42L, false));

        ReminderResponse response = reminderService.create(request);

        assertThat(response.title()).isEqualTo("과제 마감 임박");

        ArgumentCaptor<Reminder> captor = ArgumentCaptor.forClass(Reminder.class);
        verify(reminderRepository).save(captor.capture());
        assertThat(captor.getValue().getTargetAt()).isEqualTo(request.targetAt());
        assertThat(captor.getValue().getTargetDomain()).isEqualTo("pknu");
        assertThat(captor.getValue().getTargetEntityId()).isEqualTo(42L);
        assertThat(captor.getValue().getSent()).isFalse();
    }

    @Test
    void throwsWhenReminderNotFound() {
        when(reminderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updatesEveryFieldIncludingSentFlag() {
        Reminder existing = new Reminder("기존 알림", LocalDateTime.of(2026, 1, 1, 9, 0), "life", 1L, false);
        when(reminderRepository.findById(1L)).thenReturn(Optional.of(existing));

        ReminderRequest request = new ReminderRequest(
                "새 알림", LocalDateTime.of(2026, 2, 2, 13, 0), "study", 99L, true);

        ReminderResponse response = reminderService.update(1L, request);

        assertThat(response.title()).isEqualTo("새 알림");
        assertThat(response.targetAt()).isEqualTo(LocalDateTime.of(2026, 2, 2, 13, 0));
        assertThat(response.targetDomain()).isEqualTo("study");
        assertThat(response.targetEntityId()).isEqualTo(99L);
        assertThat(response.sent()).isTrue();
    }
}
