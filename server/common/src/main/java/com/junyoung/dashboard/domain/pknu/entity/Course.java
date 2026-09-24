package com.junyoung.dashboard.domain.pknu.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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

    // 이 과목의 노션 필기 페이지 주소
    @Column(length = 1000)
    private String notionUrl;

    // 성적(4.5 만점 등급: A+, A0 … D0, F). 아직 안 나왔으면 null (이슈 #134)
    @Column(length = 2)
    private String grade;

    // 과목 메모 (이슈 #134)
    @Column(columnDefinition = "TEXT")
    private String memo;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Assignment> assignments = new ArrayList<>();

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

    public void changeNotionUrl(String notionUrl) {
        this.notionUrl = notionUrl;
    }

    public void changeGradeAndMemo(String grade, String memo) {
        this.grade = grade;
        this.memo = memo;
    }
}
