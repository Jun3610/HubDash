package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.HealthLogRequest;
import com.junyoung.dashboard.domain.health.dto.HealthLogResponse;
import com.junyoung.dashboard.domain.health.entity.HealthLog;
import com.junyoung.dashboard.domain.health.repository.HealthLogRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HealthLogService {

    private final HealthLogRepository healthLogRepository;

    public HealthLogService(HealthLogRepository healthLogRepository) {
        this.healthLogRepository = healthLogRepository;
    }

    @Transactional
    public HealthLogResponse create(HealthLogRequest request) {
        HealthLog saved = healthLogRepository.save(new HealthLog(
                request.recordedAt(), request.weightKg(), request.sleepHours(), request.notes()));
        return HealthLogResponse.from(saved);
    }

    public List<HealthLogResponse> findAll() {
        return healthLogRepository.findAll().stream()
                .map(HealthLogResponse::from)
                .toList();
    }

    public HealthLogResponse findById(Long id) {
        return HealthLogResponse.from(getOrThrow(id));
    }

    @Transactional
    public HealthLogResponse update(Long id, HealthLogRequest request) {
        HealthLog log = getOrThrow(id);
        log.update(request.recordedAt(), request.weightKg(), request.sleepHours(), request.notes());
        return HealthLogResponse.from(log);
    }

    @Transactional
    public void delete(Long id) {
        healthLogRepository.delete(getOrThrow(id));
    }

    private HealthLog getOrThrow(Long id) {
        return healthLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("health log " + id + " not found"));
    }
}
