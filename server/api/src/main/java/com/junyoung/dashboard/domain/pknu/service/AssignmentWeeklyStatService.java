package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.AssignmentWeeklyStatResponse;
import com.junyoung.dashboard.domain.pknu.repository.AssignmentWeeklyStatRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AssignmentWeeklyStatService {

    private final AssignmentWeeklyStatRepository assignmentWeeklyStatRepository;

    public AssignmentWeeklyStatService(AssignmentWeeklyStatRepository assignmentWeeklyStatRepository) {
        this.assignmentWeeklyStatRepository = assignmentWeeklyStatRepository;
    }

    public Page<AssignmentWeeklyStatResponse> findByCourseId(Long courseId, Pageable pageable) {
        return assignmentWeeklyStatRepository.findByCourseId(courseId, pageable)
                .map(AssignmentWeeklyStatResponse::from);
    }
}
