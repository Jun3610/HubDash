package com.junyoung.dashboard.domain.study.dto;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class StudyProgressResponseTest {

    @Test
    void mapsEntityFieldsIncludingTopicId() {
        StudyTopic topic = new StudyTopic("토익", null);
        ReflectionTestUtils.setField(topic, "id", 1L);
        StudyProgress progress = new StudyProgress(topic, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이");

        StudyProgressResponse response = StudyProgressResponse.from(progress);

        assertThat(response.minutes()).isEqualTo(60);
        assertThat(response.notes()).isEqualTo("RC 문제풀이");
        assertThat(response.topicId()).isEqualTo(1L);
    }
}
