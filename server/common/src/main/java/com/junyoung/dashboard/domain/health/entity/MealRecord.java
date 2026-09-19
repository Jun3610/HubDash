package com.junyoung.dashboard.domain.health.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 한 끼. 영양 합계는 저장하지 않고 음식 항목(MealItem)의 합으로 계산한다.
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "health_meal_record")
public class MealRecord extends BaseEntity {

    @Column(name = "consumed_at", nullable = false)
    private LocalDateTime consumedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_type", nullable = false, length = 20)
    private MealType mealType;

    @Column(length = 500)
    private String notes;

    // 목록 조회에서 끼니마다 항목을 따로 읽는 N+1을 막기 위해 배치로 가져온다.
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "mealRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MealItem> items = new ArrayList<>();

    public MealRecord(LocalDateTime consumedAt, MealType mealType, String notes) {
        this.consumedAt = consumedAt;
        this.mealType = mealType;
        this.notes = notes;
    }

    public void update(LocalDateTime consumedAt, MealType mealType, String notes) {
        this.consumedAt = consumedAt;
        this.mealType = mealType;
        this.notes = notes;
    }
}
