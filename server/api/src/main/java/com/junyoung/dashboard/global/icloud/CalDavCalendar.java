package com.junyoung.dashboard.global.icloud;

/** 일정(VEVENT)을 담는 캘린더 하나 — url은 이 캘린더 컬렉션의 절대 주소 */
public record CalDavCalendar(String name, String url) {
}
