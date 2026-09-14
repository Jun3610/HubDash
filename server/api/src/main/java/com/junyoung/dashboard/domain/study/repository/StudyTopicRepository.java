package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyTopic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyTopicRepository extends JpaRepository<StudyTopic, Long> {
}
