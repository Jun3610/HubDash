package com.junyoung.dashboard.domain.schedule.service;

import com.junyoung.dashboard.domain.schedule.dto.EventRequest;
import com.junyoung.dashboard.domain.schedule.dto.EventResponse;
import com.junyoung.dashboard.domain.schedule.entity.Event;
import com.junyoung.dashboard.domain.schedule.repository.EventRepository;
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
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository);
    }

    @Test
    void createsEventUsingRequestFields() {
        EventRequest request = new EventRequest("발표 준비",
                LocalDateTime.of(2026, 9, 16, 10, 0), LocalDateTime.of(2026, 9, 16, 11, 0),
                "회의실 A", null, false);
        when(eventRepository.save(any(Event.class)))
                .thenReturn(new Event("발표 준비", LocalDateTime.of(2026, 9, 16, 10, 0),
                        LocalDateTime.of(2026, 9, 16, 11, 0), "회의실 A", null, false));

        EventResponse response = eventService.create(request);

        assertThat(response.title()).isEqualTo("발표 준비");

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        assertThat(captor.getValue().getStartAt()).isEqualTo(request.startAt());
        assertThat(captor.getValue().getEndAt()).isEqualTo(request.endAt());
        assertThat(captor.getValue().getAllDay()).isFalse();
    }

    @Test
    void throwsWhenEventNotFound() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void updatesEveryFieldWithoutTransposingParameters() {
        Event existing = new Event("기존 일정", LocalDateTime.of(2026, 1, 1, 9, 0),
                LocalDateTime.of(2026, 1, 1, 10, 0), "기존 장소", "기존 설명", false);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));

        EventRequest request = new EventRequest("새 일정", LocalDateTime.of(2026, 2, 2, 13, 0),
                LocalDateTime.of(2026, 2, 2, 15, 0), "새 장소", "새 설명", true);

        EventResponse response = eventService.update(1L, request);

        assertThat(response.title()).isEqualTo("새 일정");
        assertThat(response.startAt()).isEqualTo(LocalDateTime.of(2026, 2, 2, 13, 0));
        assertThat(response.endAt()).isEqualTo(LocalDateTime.of(2026, 2, 2, 15, 0));
        assertThat(response.location()).isEqualTo("새 장소");
        assertThat(response.description()).isEqualTo("새 설명");
        assertThat(response.allDay()).isTrue();
    }
}
