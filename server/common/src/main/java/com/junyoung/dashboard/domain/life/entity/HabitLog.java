package com.junyoung.dashboard.domain.life.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "life_habit_log")
public class HabitLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(name = "performed_at", nullable = false)
    private LocalDate performedAt;

    @Column(nullable = false)
    private Boolean completed;

    @Column(length = 500)
    private String notes;

    public HabitLog(Habit habit, LocalDate performedAt, Boolean completed, String notes) {
        this.habit = habit;
        this.performedAt = performedAt;
        this.completed = completed;
        this.notes = notes;
    }

    public void update(Habit habit, LocalDate performedAt, Boolean completed, String notes) {
        this.habit = habit;
        this.performedAt = performedAt;
        this.completed = completed;
        this.notes = notes;
    }
}
