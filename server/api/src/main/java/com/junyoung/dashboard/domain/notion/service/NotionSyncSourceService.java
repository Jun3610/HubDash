package com.junyoung.dashboard.domain.notion.service;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.notion.NotionIds;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncSourceRequest;
import com.junyoung.dashboard.domain.notion.dto.NotionSyncSourceResponse;
import com.junyoung.dashboard.domain.notion.entity.NotionSyncSource;
import com.junyoung.dashboard.domain.notion.repository.NotionSyncSourceRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import com.junyoung.dashboard.global.exception.InvalidRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class NotionSyncSourceService {

    private final NotionSyncSourceRepository sourceRepository;
    private final HubCategoryRepository hubCategoryRepository;

    public NotionSyncSourceService(NotionSyncSourceRepository sourceRepository,
                                   HubCategoryRepository hubCategoryRepository) {
        this.sourceRepository = sourceRepository;
        this.hubCategoryRepository = hubCategoryRepository;
    }

    @Transactional
    public NotionSyncSourceResponse create(NotionSyncSourceRequest request) {
        String notionId = NotionIds.extract(request.notion())
                .orElseThrow(() -> new InvalidRequestException("노션 ID를 찾을 수 없습니다: " + request.notion()));
        if (sourceRepository.existsByNotionId(notionId)) {
            throw new InvalidRequestException("이미 등록된 노션 소스입니다: " + notionId);
        }
        HubCategory category = hubCategoryRepository.findById(request.hubCategoryId())
                .orElseThrow(() -> EntityNotFoundException.of(HubCategory.class, request.hubCategoryId()));
        NotionSyncSource saved = sourceRepository.save(new NotionSyncSource(notionId, request.sourceType(), category));
        return NotionSyncSourceResponse.from(saved);
    }

    // 소스는 몇 개 안 되므로 페이지 없이 전부 돌려준다.
    public List<NotionSyncSourceResponse> findAll() {
        return sourceRepository.findAll().stream()
                .map(NotionSyncSourceResponse::from)
                .toList();
    }

    // 소스만 지운다 — 이미 만들어진 허브 링크는 그대로 남는다.
    @Transactional
    public void delete(Long id) {
        NotionSyncSource source = sourceRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(NotionSyncSource.class, id));
        sourceRepository.delete(source);
    }
}
