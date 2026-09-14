package com.junyoung.dashboard.domain.hub.dto;

import com.junyoung.dashboard.domain.hub.entity.HubCategory;
import com.junyoung.dashboard.domain.hub.entity.HubLink;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HubLinkResponseTest {

    @Test
    void mapsEntityFieldsIncludingCategoryId() {
        HubCategory category = new HubCategory("CI/CD", null);
        HubLink link = new HubLink(category, "Docker 문서", "https://example.com/docker", null);

        HubLinkResponse response = HubLinkResponse.from(link);

        assertThat(response.title()).isEqualTo("Docker 문서");
        assertThat(response.url()).isEqualTo("https://example.com/docker");
    }
}
