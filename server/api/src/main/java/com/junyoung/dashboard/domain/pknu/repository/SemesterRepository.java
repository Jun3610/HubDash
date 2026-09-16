package com.junyoung.dashboard.domain.pknu.repository;

import com.junyoung.dashboard.domain.pknu.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemesterRepository extends JpaRepository<Semester, Long> {
}
