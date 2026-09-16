package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.WorkoutLogRequest;
import com.junyoung.dashboard.domain.health.dto.WorkoutLogResponse;
import com.junyoung.dashboard.domain.health.entity.WorkoutLog;
import com.junyoung.dashboard.domain.health.repository.WorkoutLogRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkoutLogService {

    private final WorkoutLogRepository workoutLogRepository;

    public WorkoutLogService(WorkoutLogRepository workoutLogRepository) {
        this.workoutLogRepository = workoutLogRepository;
    }

    @Transactional
    public WorkoutLogResponse create(WorkoutLogRequest request) {
        WorkoutLog saved = workoutLogRepository.save(new WorkoutLog(
                request.performedAt(), request.type(), request.durationMinutes(),
                request.caloriesBurned(), request.notes()));
        return WorkoutLogResponse.from(saved);
    }

    public List<WorkoutLogResponse> findAll() {
        return workoutLogRepository.findAll().stream()
                .map(WorkoutLogResponse::from)
                .toList();
    }

    public WorkoutLogResponse findById(Long id) {
        return WorkoutLogResponse.from(getOrThrow(id));
    }

    @Transactional
    public WorkoutLogResponse update(Long id, WorkoutLogRequest request) {
        WorkoutLog log = getOrThrow(id);
        log.update(request.performedAt(), request.type(), request.durationMinutes(),
                request.caloriesBurned(), request.notes());
        return WorkoutLogResponse.from(log);
    }

    @Transactional
    public void delete(Long id) {
        workoutLogRepository.delete(getOrThrow(id));
    }

    private WorkoutLog getOrThrow(Long id) {
        return workoutLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("workout log " + id + " not found"));
    }
}
