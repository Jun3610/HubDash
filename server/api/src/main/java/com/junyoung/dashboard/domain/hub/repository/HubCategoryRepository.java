package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubCategoryRepository extends JpaRepository<HubCategory, Long> {
}
