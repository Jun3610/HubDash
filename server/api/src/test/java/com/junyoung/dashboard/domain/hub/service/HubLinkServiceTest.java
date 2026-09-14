package com.junyoung.dashboard.domain.hub.service;

import com.junyoung.dashboard.domain.hub.dto.HubLinkRequest;
import com.junyoung.dashboard.domain.hub.dto.HubLinkResponse;
import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import com.junyoung.dashboard.domain.hub.repository.HubCategoryRepository;
import com.junyoung.dashboard.domain.hub.repository.HubLinkRepository;
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
class HubLinkServiceTest {

    @Mock
    private HubLinkRepository hubLinkRepository;

    @Mock
    private HubCategoryRepository hubCategoryRepository;

    private HubLinkService hubLinkService;

    @BeforeEach
    void setUp() {
        hubLinkService = new HubLinkService(hubLinkRepository, hubCategoryRepository);
    }

    @Test
    void createsLinkUnderExistingCategory() {
        HubCategory category = new HubCategory("CI/CD", null);
        HubLinkRequest request = new HubLinkRequest(1L, "Docker 문서", "https://example.com/docker", null);
        when(hubCategoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(hubLinkRepository.save(any(HubLink.class)))
                .thenReturn(new HubLink(category, "Docker 문서", "https://example.com/docker", null));

        HubLinkResponse response = hubLinkService.create(request);

        assertThat(response.title()).isEqualTo("Docker 문서");
    }

    @Test
    void throwsWhenCategoryMissing() {
        HubLinkRequest request = new HubLinkRequest(1L, "Docker 문서", "https://example.com/docker", null);
        when(hubCategoryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hubLinkService.create(request))
                .isInstanceOf(EntityNotFoundException.class);
    }
}
