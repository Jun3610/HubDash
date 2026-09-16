package com.junyoung.dashboard.domain.life.service;

import com.junyoung.dashboard.domain.life.dto.ReadingLogRequest;
import com.junyoung.dashboard.domain.life.dto.ReadingLogResponse;
import com.junyoung.dashboard.domain.life.entity.ReadingLog;
import com.junyoung.dashboard.domain.life.repository.ReadingLogRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReadingLogService {

    private final ReadingLogRepository readingLogRepository;

    public ReadingLogService(ReadingLogRepository readingLogRepository) {
        this.readingLogRepository = readingLogRepository;
    }

    @Transactional
    public ReadingLogResponse create(ReadingLogRequest request) {
        ReadingLog saved = readingLogRepository.save(new ReadingLog(
                request.title(), request.author(), request.startedAt(),
                request.finishedAt(), request.rating(), request.notes()));
        return ReadingLogResponse.from(saved);
    }

    public List<ReadingLogResponse> findAll() {
        return readingLogRepository.findAll().stream()
                .map(ReadingLogResponse::from)
                .toList();
    }

    public ReadingLogResponse findById(Long id) {
        return ReadingLogResponse.from(getOrThrow(id));
    }

    @Transactional
    public ReadingLogResponse update(Long id, ReadingLogRequest request) {
        ReadingLog log = getOrThrow(id);
        log.update(request.title(), request.author(), request.startedAt(),
                request.finishedAt(), request.rating(), request.notes());
        return ReadingLogResponse.from(log);
    }

    @Transactional
    public void delete(Long id) {
        readingLogRepository.delete(getOrThrow(id));
    }

    private ReadingLog getOrThrow(Long id) {
        return readingLogRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("reading log " + id + " not found"));
    }
}
