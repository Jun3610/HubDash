package com.junyoung.dashboard.domain.notion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotionIdsTest {

    private static final String ID = "3532b9e1fd1480ffb994fe9eb920f8c3";

    @Test
    void extractsFromCompactId() {
        assertThat(NotionIds.extract(ID)).contains(ID);
    }

    @Test
    void extractsFromDashedUuidAndLowercases() {
        assertThat(NotionIds.extract("3532B9E1-FD14-80FF-B994-FE9EB920F8C3")).contains(ID);
    }

    @Test
    void extractsFromAppUrlIgnoringQuery() {
        assertThat(NotionIds.extract("https://app.notion.com/p/" + ID + "?pvs=204")).contains(ID);
    }

    @Test
    void extractsPageIdFromTitledUrlNotViewIdInQuery() {
        String url = "https://www.notion.so/workspace/Docker-" + ID + "?v=7f42b9e1fd148319b85888698a667204";
        assertThat(NotionIds.extract(url)).contains(ID);
    }

    @Test
    void emptyWhenNoId() {
        assertThat(NotionIds.extract("https://example.com/docs")).isEmpty();
        assertThat(NotionIds.extract(null)).isEmpty();
    }

    @Test
    void buildsPageUrlInSameFormatAsMigratedLinks() {
        assertThat(NotionIds.pageUrl(ID)).isEqualTo("https://app.notion.com/p/" + ID);
    }
}
