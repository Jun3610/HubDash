package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class StudyTopicRepositoryTest {

    @Autowired
    private StudyTopicRepository studyTopicRepository;

    @Test
    void savesAndAssignsIdAndTimestamps() {
        StudyTopic saved = studyTopicRepository.save(new StudyTopic("토익", "영어 공부"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
