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

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "pknu_course")
public class Course extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String professor;

    @Column(nullable = false)
    private Integer credit;

    public Course(Semester semester, String name, String professor, Integer credit) {
        this.semester = semester;
        this.name = name;
        this.professor = professor;
        this.credit = credit;
    }

    public void update(Semester semester, String name, String professor, Integer credit) {
        this.semester = semester;
        this.name = name;
        this.professor = professor;
        this.credit = credit;
    }
}
