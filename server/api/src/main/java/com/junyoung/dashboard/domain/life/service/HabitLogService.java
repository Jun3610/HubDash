package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitLogRequest;
import com.junyoung.dashboard.domain.life.dto.HabitLogResponse;
import com.junyoung.dashboard.domain.life.entity.Habit;
import com.junyoung.dashboard.domain.life.entity.HabitLog;
import com.junyoung.dashboard.domain.life.repository.HabitLogRepository;
import com.junyoung.dashboard.domain.life.repository.HabitRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HabitLogService {

    private final HabitLogRepository habitLogRepository;
    private final HabitRepository habitRepository;

    public HabitLogService(HabitLogRepository habitLogRepository, HabitRepository habitRepository) {
        this.habitLogRepository = habitLogRepository;
        this.habitRepository = habitRepository;
    }

    @Transactional
    public HabitLogResponse create(HabitLogRequest request) {
        Habit habit = getHabitOrThrow(request.habitId());
        HabitLog saved = habitLogRepository.save(
                new HabitLog(habit, request.performedAt(), request.completed(), request.notes()));
        return HabitLogResponse.from(saved);
    }

    public List<HabitLogResponse> findByHabitId(Long habitId) {
        return habitLogRepository.findByHabitId(habitId).stream()
                .map(HabitLogResponse::from)
                .toList();
    }

    public HabitLogResponse findById(Long id) {
        return HabitLogResponse.from(getOrThrow(id));
    }

    @Transactional
    public HabitLogResponse update(Long id, HabitLogRequest request) {
        HabitLog log = getOrThrow(id);
        Habit habit = getHabitOrThrow(request.habitId());
        log.update(habit, request.performedAt(), request.completed(), request.notes());
        return HabitLogResponse.from(log);
    }

    @Transactional
    public void delete(Long id) {
        habitLogRepository.delete(getOrThrow(id));
    }

    private HabitLog getOrThrow(Long id) {
        return habitLogRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(HabitLog.class, id));
    }

    private Habit getHabitOrThrow(Long habitId) {
        return habitRepository.findById(habitId)
                .orElseThrow(() -> EntityNotFoundException.of(Habit.class, habitId));
    }
}
