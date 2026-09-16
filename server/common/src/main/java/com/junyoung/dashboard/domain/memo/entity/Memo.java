package com.junyoung.dashboard.domain.memo.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "memo")
public class Memo extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Column(length = 300)
    private String tags;

    public Memo(String title, String content, String tags) {
        this.title = title;
        this.content = content;
        this.tags = tags;
    }

    public void update(String title, String content, String tags) {
        this.title = title;
        this.content = content;
        this.tags = tags;
    }
}
