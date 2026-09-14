package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.HubCategoryRequest;
import com.junyoung.dashboard.domain.hub.dto.HubCategoryResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HubCategoryServiceTest {

    @Mock
    private HubCategoryRepository hubCategoryRepository;

    private HubCategoryService hubCategoryService;

    @BeforeEach
    void setUp() {
        hubCategoryService = new HubCategoryService(hubCategoryRepository);
    }

    @Test
    void createsCategoryAndReturnsResponse() {
        HubCategoryRequest request = new HubCategoryRequest("CI/CD", "빌드 파이프라인 문서");
        when(hubCategoryRepository.save(any(HubCategory.class)))
                .thenReturn(new HubCategory("CI/CD", "빌드 파이프라인 문서"));

        HubCategoryResponse response = hubCategoryService.create(request);

        assertThat(response.name()).isEqualTo("CI/CD");
        assertThat(response.description()).isEqualTo("빌드 파이프라인 문서");
    }

    @Test
    void throwsWhenCategoryNotFound() {
        when(hubCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hubCategoryService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }
}
