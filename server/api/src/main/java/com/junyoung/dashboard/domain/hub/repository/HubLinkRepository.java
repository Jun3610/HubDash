package com.junyoung.dashboard.domain.hub.repository;

import com.junyoung.dashboard.domain.hub.entity.HubLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HubLinkRepository extends JpaRepository<HubLink, Long> {
    Page<HubLink> findByCategoryId(Long categoryId, Pageable pageable);

    /** 이미 가져온 노션 글을 가려내려고 모든 링크 주소를 읽는다 */
    @Query("select l.url from HubLink l")
    List<String> findAllUrls();
}
