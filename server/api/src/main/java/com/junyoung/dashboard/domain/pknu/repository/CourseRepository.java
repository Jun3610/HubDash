package com.junyoung.dashboard.domain.pknu.repository;

import com.junyoung.dashboard.domain.pknu.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySemesterId(Long semesterId);
}
