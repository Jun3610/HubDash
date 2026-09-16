package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitRequest;
import com.junyoung.dashboard.domain.life.dto.HabitResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    public List<HabitResponse> findAll() {
        return habitRepository.findAll().stream()
                .map(HabitResponse::from)
                .toList();
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
                .orElseThrow(() -> new EntityNotFoundException("habit " + id + " not found"));
    }
}
