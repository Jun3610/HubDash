package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyProgressRepository extends JpaRepository<StudyProgress, Long> {
    List<StudyProgress> findByTopicId(Long topicId);
}
