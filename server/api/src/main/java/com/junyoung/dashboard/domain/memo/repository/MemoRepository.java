package com.junyoung.dashboard.domain.memo.repository;

import com.junyoung.dashboard.domain.memo.entity.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemoRepository extends JpaRepository<Memo, Long> {
}
