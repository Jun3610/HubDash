package com.junyoung.dashboard.domain.schedule.controller;

import com.junyoung.dashboard.domain.schedule.dto.EventRequest;
import com.junyoung.dashboard.domain.schedule.dto.EventResponse;
import com.junyoung.dashboard.domain.schedule.service.EventService;
import com.junyoung.dashboard.global.common.ApiResponse;
import com.junyoung.dashboard.global.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schedule/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<EventResponse> create(@Valid @RequestBody EventRequest request) {
        return ApiResponse.success(eventService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<EventResponse>> findAll(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.of(eventService.findAll(pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<EventResponse> findById(@PathVariable Long id) {
        return ApiResponse.success(eventService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<EventResponse> update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return ApiResponse.success(eventService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        eventService.delete(id);
    }
}
