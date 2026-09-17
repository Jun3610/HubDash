package com.junyoung.dashboard.domain.study.repository;

import com.junyoung.dashboard.domain.study.entity.RawStudyProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawStudyProgressRepository extends JpaRepository<RawStudyProgress, Long> {
}
