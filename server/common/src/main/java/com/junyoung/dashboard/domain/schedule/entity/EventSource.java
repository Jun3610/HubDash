package com.junyoung.dashboard.domain.schedule.entity;

/** 일정이 어디서 왔는지 (이슈 #228). MANUAL은 HubDash에서 직접 만든 것 — 동기화가 건드리지 않는다 */
public enum EventSource {
    MANUAL,
    NOTION,
    ICLOUD
}
