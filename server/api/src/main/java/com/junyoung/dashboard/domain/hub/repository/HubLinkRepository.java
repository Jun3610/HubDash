package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HubLinkRepository extends JpaRepository<HubLink, Long> {
    Page<HubLink> findByCategoryId(Long categoryId, Pageable pageable);
}
