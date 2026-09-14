package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HubCategoryResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        HubCategory category = new HubCategory("CI/CD", "빌드 파이프라인 문서");

        HubCategoryResponse response = HubCategoryResponse.from(category);

        assertThat(response.name()).isEqualTo("CI/CD");
        assertThat(response.description()).isEqualTo("빌드 파이프라인 문서");
    }
}
