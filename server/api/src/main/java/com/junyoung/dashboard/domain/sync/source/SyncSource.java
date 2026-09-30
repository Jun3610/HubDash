package com.junyoung.dashboard.domain.sync.source;

import com.junyoung.dashboard.domain.schedule.entity.EventSource;

import java.util.List;

/**
 * 일정을 가져올 곳 하나 (이슈 #228). 가져오기만 하고 원본에는 쓰지 않는다.
 * fetch()는 그 소스의 "지금 있는 일정 전부"를 돌려줘야 한다 — 여기에 없는 것은 지워진 것으로 본다.
 */
public interface SyncSource {

    EventSource source();

    /** 결과·기록에 쓰는 이름 */
    default String name() {
        return source().name();
    }

    /** 계정 정보와 설정이 다 있어서 돌릴 수 있는지 */
    boolean configured();

    List<ExternalEvent> fetch();
}
