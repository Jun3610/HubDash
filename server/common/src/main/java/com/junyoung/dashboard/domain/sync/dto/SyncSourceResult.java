package com.junyoung.dashboard.domain.sync.dto;

/** 소스 하나의 동기화 결과. ok=false면 error에 이유 (다른 소스는 계속 돈다) */
public record SyncSourceResult(String name, boolean ok, int added, int updated, int deleted, String error) {

    public static SyncSourceResult failed(String name, String error) {
        return new SyncSourceResult(name, false, 0, 0, 0, error);
    }
}
