package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import com.junyoung.dashboard.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class StudyProgressRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private StudyProgressRepository studyProgressRepository;

    @Test
    void findsProgressesByTopicId() {
        StudyTopic topic = entityManager.persistAndFlush(new StudyTopic("토익", null));
        entityManager.persistAndFlush(new StudyProgress(topic, LocalDate.of(2026, 9, 14), 60, "RC 문제풀이"));

        Page<StudyProgress> progresses = studyProgressRepository.findByTopicId(topic.getId(), Pageable.unpaged());

        assertThat(progresses.getContent()).hasSize(1);
        assertThat(progresses.getContent().get(0).getMinutes()).isEqualTo(60);
    }
}
