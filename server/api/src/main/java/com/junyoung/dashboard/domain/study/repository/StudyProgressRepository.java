package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.StudyProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface StudyProgressRepository extends JpaRepository<StudyProgress, Long> {
    Page<StudyProgress> findByTopicId(Long topicId, Pageable pageable);

    @Query("SELECT DISTINCT p.topic.id FROM StudyProgress p WHERE p.studiedAt BETWEEN :weekStart AND :weekEnd")
    List<Long> findDistinctTopicIdsWithProgressBetween(LocalDate weekStart, LocalDate weekEnd);

    long countByTopicIdAndStudiedAtBetween(Long topicId, LocalDate weekStart, LocalDate weekEnd);

    @Query("SELECT COALESCE(SUM(p.minutes), 0) FROM StudyProgress p "
            + "WHERE p.topic.id = :topicId AND p.studiedAt BETWEEN :weekStart AND :weekEnd")
    long sumMinutesByTopicIdAndStudiedAtBetween(Long topicId, LocalDate weekStart, LocalDate weekEnd);
}
