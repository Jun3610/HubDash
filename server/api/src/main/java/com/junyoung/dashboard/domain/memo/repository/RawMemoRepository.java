package com.junyoung.dashboard.domain.memo.repository;

import com.junyoung.dashboard.domain.memo.entity.RawMemo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawMemoRepository extends JpaRepository<RawMemo, Long> {
}
