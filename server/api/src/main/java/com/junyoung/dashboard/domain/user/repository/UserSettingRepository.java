package com.junyoung.dashboard.domain.user.repository;

import com.junyoung.dashboard.domain.user.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, Long> {
}
