package com.junyoung.dashboard.domain.schedule.service;

import com.junyoung.dashboard.domain.schedule.dto.EventRequest;
import com.junyoung.dashboard.domain.schedule.dto.EventResponse;
import com.junyoung.dashboard.domain.schedule.entity.Event;
import com.junyoung.dashboard.domain.schedule.repository.EventRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional
    public EventResponse create(EventRequest request) {
        Event saved = eventRepository.save(new Event(
                request.title(), request.startAt(), request.endAt(),
                request.location(), request.description(), request.allDay()));
        return EventResponse.from(saved);
    }

    public List<EventResponse> findAll() {
        return eventRepository.findAll().stream()
                .map(EventResponse::from)
                .toList();
    }

    public EventResponse findById(Long id) {
        return EventResponse.from(getOrThrow(id));
    }

    @Transactional
    public EventResponse update(Long id, EventRequest request) {
        Event event = getOrThrow(id);
        event.update(request.title(), request.startAt(), request.endAt(),
                request.location(), request.description(), request.allDay());
        return EventResponse.from(event);
    }

    @Transactional
    public void delete(Long id) {
        eventRepository.delete(getOrThrow(id));
    }

    private Event getOrThrow(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("event " + id + " not found"));
    }
}
