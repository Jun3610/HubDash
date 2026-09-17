package com.junyoung.dashboard.domain.memo.entity;

import com.junyoung.dashboard.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "memo_raw")
public class RawMemo extends BaseEntity {

    @Column(name = "title_raw", nullable = false, length = 200)
    private String titleRaw;

    @Column(name = "content_raw", nullable = false, length = 5000)
    private String contentRaw;

    @Column(length = 300)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawMemo(String titleRaw, String contentRaw, String tags) {
        this.titleRaw = titleRaw;
        this.contentRaw = contentRaw;
        this.tags = tags;
        this.status = RawStatus.PENDING;
    }

    public void markProcessed() {
        this.status = RawStatus.PROCESSED;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = RawStatus.FAILED;
        this.failureReason = reason;
    }
}
