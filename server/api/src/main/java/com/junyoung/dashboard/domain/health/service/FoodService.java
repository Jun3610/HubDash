package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.FoodRequest;
import com.junyoung.dashboard.domain.health.dto.FoodResponse;
import com.junyoung.dashboard.domain.health.entity.Food;
import com.junyoung.dashboard.domain.health.repository.FoodRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FoodService {

    /** 정렬을 따로 주지 않으면 고정한 음식 먼저, 그다음 이름 순 */
    static final Sort DEFAULT_SORT = Sort.by(Sort.Order.desc("pinned"), Sort.Order.asc("name"));

    private final FoodRepository foodRepository;

    public FoodService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    @Transactional
    public FoodResponse create(FoodRequest request) {
        Food saved = foodRepository.save(new Food(
                request.name(), request.calories(), request.carbsG(), request.fatG(),
                request.proteinG(), request.serving(), request.pinnedOrFalse()));
        return FoodResponse.from(saved);
    }

    public Page<FoodResponse> findAll(Pageable pageable) {
        Pageable sorted = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_SORT);
        return foodRepository.findAll(sorted).map(FoodResponse::from);
    }

    public FoodResponse findById(Long id) {
        return FoodResponse.from(getOrThrow(id));
    }

    @Transactional
    public FoodResponse update(Long id, FoodRequest request) {
        Food food = getOrThrow(id);
        food.update(request.name(), request.calories(), request.carbsG(), request.fatG(),
                request.proteinG(), request.serving(), request.pinnedOrFalse());
        return FoodResponse.from(food);
    }

    @Transactional
    public void delete(Long id) {
        foodRepository.delete(getOrThrow(id));
    }

    private Food getOrThrow(Long id) {
        return foodRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(Food.class, id));
    }
}
