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
@Table(name = "user_profile")
public class UserProfile extends BaseEntity {

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(length = 200)
    private String email;

    @Column(length = 500)
    private String bio;

    public UserProfile(String displayName, String email, String bio) {
        this.displayName = displayName;
        this.email = email;
        this.bio = bio;
    }

    public void update(String displayName, String email, String bio) {
        this.displayName = displayName;
        this.email = email;
        this.bio = bio;
    }
}
