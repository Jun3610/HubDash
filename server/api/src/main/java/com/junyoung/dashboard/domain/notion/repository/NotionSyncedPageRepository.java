package com.junyoung.dashboard.domain.notion.repository;

import com.junyoung.dashboard.domain.notion.entity.NotionSyncedPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface NotionSyncedPageRepository extends JpaRepository<NotionSyncedPage, Long> {

    @Query("select p.notionPageId from NotionSyncedPage p where p.source.id = :sourceId")
    Set<String> findNotionPageIdsBySourceId(Long sourceId);
}
