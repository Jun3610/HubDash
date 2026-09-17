package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyTopicWeeklyStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface StudyTopicWeeklyStatRepository extends JpaRepository<StudyTopicWeeklyStat, Long> {
    Optional<StudyTopicWeeklyStat> findByTopicIdAndWeekStart(Long topicId, LocalDate weekStart);

    Page<StudyTopicWeeklyStat> findByTopicId(Long topicId, Pageable pageable);
}
