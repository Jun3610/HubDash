package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.MealRecordRequest;
import com.junyoung.dashboard.domain.health.dto.MealRecordResponse;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.repository.MealRecordRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MealRecordService {

    private final MealRecordRepository mealRecordRepository;

    public MealRecordService(MealRecordRepository mealRecordRepository) {
        this.mealRecordRepository = mealRecordRepository;
    }

    @Transactional
    public MealRecordResponse create(MealRecordRequest request) {
        MealRecord saved = mealRecordRepository.save(new MealRecord(
                request.consumedAt(), request.mealType(), request.calories(),
                request.carbsG(), request.proteinG(), request.fatG(), request.sodiumMg(), request.notes()));
        return MealRecordResponse.from(saved);
    }

    public List<MealRecordResponse> findAll() {
        return mealRecordRepository.findAll().stream()
                .map(MealRecordResponse::from)
                .toList();
    }

    public MealRecordResponse findById(Long id) {
        return MealRecordResponse.from(getOrThrow(id));
    }

    @Transactional
    public MealRecordResponse update(Long id, MealRecordRequest request) {
        MealRecord record = getOrThrow(id);
        record.update(request.consumedAt(), request.mealType(), request.calories(),
                request.carbsG(), request.proteinG(), request.fatG(), request.sodiumMg(), request.notes());
        return MealRecordResponse.from(record);
    }

    @Transactional
    public void delete(Long id) {
        mealRecordRepository.delete(getOrThrow(id));
    }

    private MealRecord getOrThrow(Long id) {
        return mealRecordRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(MealRecord.class, id));
    }
}
