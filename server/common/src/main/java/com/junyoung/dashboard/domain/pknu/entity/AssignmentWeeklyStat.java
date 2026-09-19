package com.junyoung.dashboard.domain.pknu.entity;

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

// 집계 기준은 Assignment.dueDate — weekStart 주에 마감인 과제의 총 수와 그중 완료된 수.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pknu_assignment_weekly_stat",
        uniqueConstraints = @UniqueConstraint(columnNames = {"course_id", "week_start"}))
public class AssignmentWeeklyStat extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "completed_count", nullable = false)
    private Integer completedCount;

    public AssignmentWeeklyStat(Course course, LocalDate weekStart, Integer totalCount, Integer completedCount) {
        this.course = course;
        this.weekStart = weekStart;
        this.totalCount = totalCount;
        this.completedCount = completedCount;
    }

    public void updateCounts(Integer totalCount, Integer completedCount) {
        this.totalCount = totalCount;
        this.completedCount = completedCount;
    }
}
