package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudyTopicResponseTest {

    @Test
    void mapsEntityFieldsToResponse() {
        StudyTopic topic = new StudyTopic("토익", "영어 공부");

        StudyTopicResponse response = StudyTopicResponse.from(topic);

        assertThat(response.name()).isEqualTo("토익");
        assertThat(response.description()).isEqualTo("영어 공부");
    }
}
