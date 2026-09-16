package com.junyoung.dashboard.domain.pknu.entity;

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
@Table(name = "pknu_assignment")
public class Assignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private Boolean completed;

    @Column(length = 500)
    private String notes;

    public Assignment(Course course, String title, LocalDate dueDate, Boolean completed, String notes) {
        this.course = course;
        this.title = title;
        this.dueDate = dueDate;
        this.completed = completed;
        this.notes = notes;
    }

    public void update(Course course, String title, LocalDate dueDate, Boolean completed, String notes) {
        this.course = course;
        this.title = title;
        this.dueDate = dueDate;
        this.completed = completed;
        this.notes = notes;
    }
}
