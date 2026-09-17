package com.junyoung.dashboard.domain.hub.entity;

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
@Table(name = "hub_raw_link")
public class RawHubLink extends BaseEntity {

    @Column(name = "category_id_raw", nullable = false, length = 50)
    private String categoryIdRaw;

    @Column(name = "title_raw", nullable = false, length = 200)
    private String titleRaw;

    @Column(name = "url_raw", nullable = false, length = 1000)
    private String urlRaw;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RawStatus status;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    public RawHubLink(String categoryIdRaw, String titleRaw, String urlRaw, String description) {
        this.categoryIdRaw = categoryIdRaw;
        this.titleRaw = titleRaw;
        this.urlRaw = urlRaw;
        this.description = description;
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
