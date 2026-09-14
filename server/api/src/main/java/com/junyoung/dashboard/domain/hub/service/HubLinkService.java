package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.HubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.HubLinkResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HubLinkService {

    private final HubLinkRepository hubLinkRepository;
    private final HubCategoryRepository hubCategoryRepository;

    public HubLinkService(HubLinkRepository hubLinkRepository, HubCategoryRepository hubCategoryRepository) {
        this.hubLinkRepository = hubLinkRepository;
        this.hubCategoryRepository = hubCategoryRepository;
    }

    @Transactional
    public HubLinkResponse create(HubLinkRequest request) {
        HubCategory category = hubCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("hub category " + request.categoryId() + " not found"));
        HubLink saved = hubLinkRepository.save(
                new HubLink(category, request.title(), request.url(), request.description()));
        return HubLinkResponse.from(saved);
    }

    public List<HubLinkResponse> findByCategoryId(Long categoryId) {
        return hubLinkRepository.findByCategoryId(categoryId).stream()
                .map(HubLinkResponse::from)
                .toList();
    }

    public HubLinkResponse findById(Long id) {
        return HubLinkResponse.from(getOrThrow(id));
    }

    @Transactional
    public HubLinkResponse update(Long id, HubLinkRequest request) {
        HubLink link = getOrThrow(id);
        HubCategory category = hubCategoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new EntityNotFoundException("hub category " + request.categoryId() + " not found"));
        link.update(request.title(), request.url(), request.description());
        link.changeCategory(category);
        return HubLinkResponse.from(link);
    }

    @Transactional
    public void delete(Long id) {
        hubLinkRepository.delete(getOrThrow(id));
    }

    private HubLink getOrThrow(Long id) {
        return hubLinkRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("hub link " + id + " not found"));
    }
}
