package com.junyoung.dashboard.domain.notion.entity;

// DATABASE는 DB의 행(페이지)을, PAGE는 부모 페이지 바로 아래 하위 페이지를 동기화 대상으로 삼는다.
public enum NotionSourceType {
    DATABASE,
    PAGE
}
