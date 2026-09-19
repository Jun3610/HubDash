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

// 평균값은 해당 주에 체중/수면 값이 하나도 없으면 null로 남긴다(0으로 채우면 실제 측정값과 구분되지 않음).
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_log_weekly_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"week_start"}))
public class HealthLogWeeklyStat extends BaseEntity {

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "log_count", nullable = false)
    private Integer logCount;

    @Column(name = "avg_weight_kg")
    private Double avgWeightKg;

    @Column(name = "avg_sleep_hours")
    private Double avgSleepHours;

    public HealthLogWeeklyStat(LocalDate weekStart, Integer logCount, Double avgWeightKg, Double avgSleepHours) {
        this.weekStart = weekStart;
        this.logCount = logCount;
        this.avgWeightKg = avgWeightKg;
        this.avgSleepHours = avgSleepHours;
    }

    public void updateStats(Integer logCount, Double avgWeightKg, Double avgSleepHours) {
        this.logCount = logCount;
        this.avgWeightKg = avgWeightKg;
        this.avgSleepHours = avgSleepHours;
    }
}
