package com.junyoung.dashboard.domain.health.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 하루 합계(MealTotals)의 주간 일평균. 음식을 하나라도 기록한 날만 dayCount에 포함한다 —
// 기록이 없는 날을 0으로 평균에 넣으면 "안 먹은 날"과 "안 적은 날"이 구분되지 않아 평균이 왜곡된다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_meal_weekly_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"week_start"}))
public class MealWeeklyStat extends BaseEntity {

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "day_count", nullable = false)
    private Integer dayCount;

    @Column(name = "avg_calories", nullable = false)
    private Double avgCalories;

    @Column(name = "avg_carbs_g", nullable = false)
    private Double avgCarbsG;

    @Column(name = "avg_protein_g", nullable = false)
    private Double avgProteinG;

    @Column(name = "avg_fat_g", nullable = false)
    private Double avgFatG;

    public MealWeeklyStat(LocalDate weekStart, Integer dayCount, Double avgCalories,
                           Double avgCarbsG, Double avgProteinG, Double avgFatG) {
        this.weekStart = weekStart;
        this.dayCount = dayCount;
        this.avgCalories = avgCalories;
        this.avgCarbsG = avgCarbsG;
        this.avgProteinG = avgProteinG;
        this.avgFatG = avgFatG;
    }

    public void updateStats(Integer dayCount, Double avgCalories, Double avgCarbsG, Double avgProteinG, Double avgFatG) {
        this.dayCount = dayCount;
        this.avgCalories = avgCalories;
        this.avgCarbsG = avgCarbsG;
        this.avgProteinG = avgProteinG;
        this.avgFatG = avgFatG;
    }
}
