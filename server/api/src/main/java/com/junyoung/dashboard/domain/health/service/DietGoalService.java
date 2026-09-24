package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.DietGoalRequest;
import com.junyoung.dashboard.domain.health.dto.DietGoalResponse;
import com.junyoung.dashboard.domain.health.entity.DietGoal;
import com.junyoung.dashboard.domain.health.repository.DietGoalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class DietGoalService {

    private final DietGoalRepository dietGoalRepository;

    public DietGoalService(DietGoalRepository dietGoalRepository) {
        this.dietGoalRepository = dietGoalRepository;
    }

    @Transactional
    public DietGoalResponse get() {
        return DietGoalResponse.from(getOrCreate());
    }

    @Transactional
    public DietGoalResponse update(DietGoalRequest request) {
        DietGoal goal = getOrCreate();
        goal.update(request.carbsG(), request.carbsRule(), request.fatG(), request.fatRule(),
                request.proteinG(), request.proteinRule(), request.calories(), request.caloriesRule());
        return DietGoalResponse.from(goal);
    }

    // 사용자 설정과 같은 방식: 첫 조회 때 목표가 비어 있는 행을 만든다
    private DietGoal getOrCreate() {
        List<DietGoal> existing = dietGoalRepository.findAll();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return dietGoalRepository.save(DietGoal.empty());
    }
}
