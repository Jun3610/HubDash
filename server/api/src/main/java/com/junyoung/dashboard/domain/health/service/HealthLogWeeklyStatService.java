package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.HealthLogWeeklyStatResponse;
import com.junyoung.dashboard.domain.health.repository.HealthLogWeeklyStatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HealthLogWeeklyStatService {

    private final HealthLogWeeklyStatRepository healthLogWeeklyStatRepository;

    public HealthLogWeeklyStatService(HealthLogWeeklyStatRepository healthLogWeeklyStatRepository) {
        this.healthLogWeeklyStatRepository = healthLogWeeklyStatRepository;
    }

    public Page<HealthLogWeeklyStatResponse> findAll(Pageable pageable) {
        return healthLogWeeklyStatRepository.findAllByOrderByWeekStartDesc(pageable)
                .map(HealthLogWeeklyStatResponse::from);
    }
}
