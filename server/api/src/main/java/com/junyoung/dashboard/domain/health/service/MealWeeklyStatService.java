package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.MealWeeklyStatResponse;
import com.junyoung.dashboard.domain.health.repository.MealWeeklyStatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MealWeeklyStatService {

    private final MealWeeklyStatRepository mealWeeklyStatRepository;

    public MealWeeklyStatService(MealWeeklyStatRepository mealWeeklyStatRepository) {
        this.mealWeeklyStatRepository = mealWeeklyStatRepository;
    }

    public Page<MealWeeklyStatResponse> findAll(Pageable pageable) {
        return mealWeeklyStatRepository.findAllByOrderByWeekStartDesc(pageable)
                .map(MealWeeklyStatResponse::from);
    }
}
