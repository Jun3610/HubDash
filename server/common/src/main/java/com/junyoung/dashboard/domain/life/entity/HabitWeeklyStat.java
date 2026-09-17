package com.junyoung.dashboard.domain.life.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "life_habit_weekly_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "week_start"}))
public class HabitWeeklyStat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "completed_count", nullable = false)
    private Integer completedCount;

    public HabitWeeklyStat(Habit habit, LocalDate weekStart, Integer totalCount, Integer completedCount) {
        this.habit = habit;
        this.weekStart = weekStart;
        this.totalCount = totalCount;
        this.completedCount = completedCount;
    }

    public void updateCounts(Integer totalCount, Integer completedCount) {
        this.totalCount = totalCount;
        this.completedCount = completedCount;
    }
}
