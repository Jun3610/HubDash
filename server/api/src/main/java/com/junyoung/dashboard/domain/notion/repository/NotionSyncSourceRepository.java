package com.junyoung.dashboard.domain.notion.repository;

import com.junyoung.dashboard.domain.notion.entity.NotionSyncSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotionSyncSourceRepository extends JpaRepository<NotionSyncSource, Long> {
    boolean existsByNotionId(String notionId);
}
