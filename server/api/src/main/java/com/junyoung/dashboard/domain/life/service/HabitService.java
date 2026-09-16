package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HabitService {

    private final HabitRepository habitRepository;

    public HabitService(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    @Transactional
    public HabitResponse create(HabitRequest request) {
        Habit saved = habitRepository.save(new Habit(request.name(), request.description()));
        return HabitResponse.from(saved);
    }

    public Page<HabitResponse> findAll(Pageable pageable) {
        return habitRepository.findAll(pageable)
                .map(HabitResponse::from);
    }

    public HabitResponse findById(Long id) {
        return HabitResponse.from(getOrThrow(id));
    }

    @Transactional
    public HabitResponse update(Long id, HabitRequest request) {
        Habit habit = getOrThrow(id);
        habit.update(request.name(), request.description());
        return HabitResponse.from(habit);
    }

    @Transactional
    public void delete(Long id) {
        habitRepository.delete(getOrThrow(id));
    }

    private Habit getOrThrow(Long id) {
        return habitRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(Habit.class, id));
    }
}
