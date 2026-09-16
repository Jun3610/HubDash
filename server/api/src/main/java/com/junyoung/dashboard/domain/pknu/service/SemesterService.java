package com.junyoung.dashboard.domain.pknu.service;

import com.junyoung.dashboard.domain.pknu.dto.SemesterRequest;
import com.junyoung.dashboard.domain.pknu.dto.SemesterResponse;
import com.junyoung.dashboard.domain.pknu.entity.Semester;
import com.junyoung.dashboard.domain.pknu.repository.SemesterRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SemesterService {

    private final SemesterRepository semesterRepository;

    public SemesterService(SemesterRepository semesterRepository) {
        this.semesterRepository = semesterRepository;
    }

    @Transactional
    public SemesterResponse create(SemesterRequest request) {
        Semester saved = semesterRepository.save(
                new Semester(request.name(), request.startDate(), request.endDate()));
        return SemesterResponse.from(saved);
    }

    public List<SemesterResponse> findAll() {
        return semesterRepository.findAll().stream()
                .map(SemesterResponse::from)
                .toList();
    }

    public SemesterResponse findById(Long id) {
        return SemesterResponse.from(getOrThrow(id));
    }

    @Transactional
    public SemesterResponse update(Long id, SemesterRequest request) {
        Semester semester = getOrThrow(id);
        semester.update(request.name(), request.startDate(), request.endDate());
        return SemesterResponse.from(semester);
    }

    @Transactional
    public void delete(Long id) {
        semesterRepository.delete(getOrThrow(id));
    }

    private Semester getOrThrow(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(Semester.class, id));
    }
}
