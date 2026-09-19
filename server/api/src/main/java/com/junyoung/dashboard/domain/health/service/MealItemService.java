package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.MealItemRequest;
import com.junyoung.dashboard.domain.health.dto.MealItemResponse;
import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.repository.MealItemRepository;
import com.junyoung.dashboard.domain.health.repository.MealRecordRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MealItemService {

    private final MealItemRepository mealItemRepository;
    private final MealRecordRepository mealRecordRepository;

    public MealItemService(MealItemRepository mealItemRepository, MealRecordRepository mealRecordRepository) {
        this.mealItemRepository = mealItemRepository;
        this.mealRecordRepository = mealRecordRepository;
    }

    @Transactional
    public MealItemResponse create(MealItemRequest request) {
        MealRecord mealRecord = getMealRecordOrThrow(request.mealRecordId());
        MealItem saved = mealItemRepository.save(new MealItem(mealRecord, request.name(), request.calories(),
                request.carbsG(), request.proteinG(), request.fatG(), request.sodiumMg()));
        return MealItemResponse.from(saved);
    }

    public Page<MealItemResponse> findByMealRecordId(Long mealRecordId, Pageable pageable) {
        return mealItemRepository.findByMealRecordId(mealRecordId, pageable)
                .map(MealItemResponse::from);
    }

    public MealItemResponse findById(Long id) {
        return MealItemResponse.from(getOrThrow(id));
    }

    @Transactional
    public MealItemResponse update(Long id, MealItemRequest request) {
        MealItem item = getOrThrow(id);
        MealRecord mealRecord = getMealRecordOrThrow(request.mealRecordId());
        item.update(mealRecord, request.name(), request.calories(),
                request.carbsG(), request.proteinG(), request.fatG(), request.sodiumMg());
        return MealItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        mealItemRepository.delete(getOrThrow(id));
    }

    private MealItem getOrThrow(Long id) {
        return mealItemRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(MealItem.class, id));
    }

    private MealRecord getMealRecordOrThrow(Long mealRecordId) {
        return mealRecordRepository.findById(mealRecordId)
                .orElseThrow(() -> EntityNotFoundException.of(MealRecord.class, mealRecordId));
    }
}
