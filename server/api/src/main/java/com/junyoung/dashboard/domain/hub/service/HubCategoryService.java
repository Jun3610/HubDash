package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class HubCategoryService {

    private final HubCategoryRepository hubCategoryRepository;

    public HubCategoryService(HubCategoryRepository hubCategoryRepository) {
        this.hubCategoryRepository = hubCategoryRepository;
    }

    @Transactional
    public HubCategoryResponse create(HubCategoryRequest request) {
        HubCategory saved = hubCategoryRepository.save(
                new HubCategory(request.name(), request.description()));
        return HubCategoryResponse.from(saved);
    }

    public List<HubCategoryResponse> findAll() {
        return hubCategoryRepository.findAll().stream()
                .map(HubCategoryResponse::from)
                .toList();
    }

    public HubCategoryResponse findById(Long id) {
        return HubCategoryResponse.from(getOrThrow(id));
    }

    @Transactional
    public HubCategoryResponse update(Long id, HubCategoryRequest request) {
        HubCategory category = getOrThrow(id);
        category.update(request.name(), request.description());
        return HubCategoryResponse.from(category);
    }

    @Transactional
    public void delete(Long id) {
        hubCategoryRepository.delete(getOrThrow(id));
    }

    private HubCategory getOrThrow(Long id) {
        return hubCategoryRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(HubCategory.class, id));
    }
}
