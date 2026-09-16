package com.junyoung.dashboard.domain.pknu.repository;

import com.junyoung.dashboard.domain.pknu.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findBySemesterId(Long semesterId, Pageable pageable);
}
