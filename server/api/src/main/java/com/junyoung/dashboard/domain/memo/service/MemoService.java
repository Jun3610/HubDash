package com.junyoung.dashboard.domain.memo.service;

import com.junyoung.dashboard.domain.memo.dto.MemoRequest;
import com.junyoung.dashboard.domain.memo.dto.MemoResponse;
import com.junyoung.dashboard.domain.memo.entity.Memo;
import com.junyoung.dashboard.domain.memo.repository.MemoRepository;
import com.junyoung.dashboard.global.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;

    public MemoService(MemoRepository memoRepository) {
        this.memoRepository = memoRepository;
    }

    @Transactional
    public MemoResponse create(MemoRequest request) {
        Memo saved = memoRepository.save(new Memo(request.title(), request.content(), request.tags()));
        return MemoResponse.from(saved);
    }

    public List<MemoResponse> findAll() {
        return memoRepository.findAll().stream()
                .map(MemoResponse::from)
                .toList();
    }

    public MemoResponse findById(Long id) {
        return MemoResponse.from(getOrThrow(id));
    }

    @Transactional
    public MemoResponse update(Long id, MemoRequest request) {
        Memo memo = getOrThrow(id);
        memo.update(request.title(), request.content(), request.tags());
        return MemoResponse.from(memo);
    }

    @Transactional
    public void delete(Long id) {
        memoRepository.delete(getOrThrow(id));
    }

    private Memo getOrThrow(Long id) {
        return memoRepository.findById(id)
                .orElseThrow(() -> EntityNotFoundException.of(Memo.class, id));
    }
}
