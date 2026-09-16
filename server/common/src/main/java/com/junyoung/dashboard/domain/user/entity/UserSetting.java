package com.junyoung.dashboard.domain.user.entity;

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
@Table(name = "user_setting")
public class UserSetting extends BaseEntity {

    @Column(nullable = false, length = 20)
    private String theme;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

    public UserSetting(String theme, String language, Boolean notificationEnabled) {
        this.theme = theme;
        this.language = language;
        this.notificationEnabled = notificationEnabled;
    }

    public void update(String theme, String language, Boolean notificationEnabled) {
        this.theme = theme;
        this.language = language;
        this.notificationEnabled = notificationEnabled;
    }
}
