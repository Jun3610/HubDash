package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.HabitWeeklyStatResponse;
import com.junyoung.dashboard.domain.life.repository.HabitWeeklyStatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HabitWeeklyStatService {

    private final HabitWeeklyStatRepository habitWeeklyStatRepository;

    public HabitWeeklyStatService(HabitWeeklyStatRepository habitWeeklyStatRepository) {
        this.habitWeeklyStatRepository = habitWeeklyStatRepository;
    }

    public Page<HabitWeeklyStatResponse> findByHabitId(Long habitId, Pageable pageable) {
        return habitWeeklyStatRepository.findByHabitId(habitId, pageable)
                .map(HabitWeeklyStatResponse::from);
    }
}
