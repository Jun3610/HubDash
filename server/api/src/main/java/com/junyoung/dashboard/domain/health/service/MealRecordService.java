package com.junyoung.dashboard.domain.health.service;

import com.junyoung.dashboard.domain.health.dto.DailyMealSummaryResponse;
import com.junyoung.dashboard.domain.health.dto.DailyMealSummaryResponse.MealTypeSummary;
import com.junyoung.dashboard.domain.health.dto.MealRecordRequest;
import com.junyoung.dashboard.domain.health.dto.MealRecordResponse;
import com.junyoung.dashboard.domain.health.dto.MealTotals;
import com.junyoung.dashboard.domain.health.entity.MealItem;
import com.junyoung.dashboard.domain.health.entity.MealRecord;
import com.junyoung.dashboard.domain.health.entity.MealType;
import com.junyoung.dashboard.domain.health.repository.MealRecordRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
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
        MealRecord saved = mealRecordRepository.save(
                new MealRecord(request.consumedAt(), request.mealType(), request.notes()));
        return MealRecordResponse.from(saved);
    }

    public Page<MealRecordResponse> findAll(Pageable pageable) {
        return mealRecordRepository.findAll(pageable)
                .map(MealRecordResponse::from);
    }

    public MealRecordResponse findById(Long id) {
        return MealRecordResponse.from(getOrThrow(id));
    }

    public DailyMealSummaryResponse dailySummary(LocalDate date) {
        List<MealRecord> records = mealRecordRepository.findByConsumedAtGreaterThanEqualAndConsumedAtLessThan(
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        List<MealTypeSummary> meals = Arrays.stream(MealType.values())
                .map(type -> {
                    List<MealItem> items = records.stream()
                            .filter(record -> record.getMealType() == type)
                            .flatMap(record -> record.getItems().stream())
                            .toList();
                    return new MealTypeSummary(type, items.size(), MealTotals.of(items));
                })
                .toList();

        List<MealItem> allItems = records.stream().flatMap(record -> record.getItems().stream()).toList();
        return new DailyMealSummaryResponse(date, MealTotals.of(allItems), meals);
    }

    @Transactional
    public MealRecordResponse update(Long id, MealRecordRequest request) {
        MealRecord record = getOrThrow(id);
        record.update(request.consumedAt(), request.mealType(), request.notes());
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
