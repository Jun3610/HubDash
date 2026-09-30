package com.junyoung.dashboard.domain.integration.dto;

/** iCloud 캘린더 하나 — 웹 체크박스용. duplicateWarning이 있으면 가져오면 중복된다는 안내 */
public record CalendarOption(String name, boolean selected, String duplicateWarning) {
}
