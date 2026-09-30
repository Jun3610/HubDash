package com.junyoung.dashboard.global.icloud;

/** CalDAV 로그인 정보 — 앱 암호는 appleid.apple.com에서 만든 "앱 전용 암호"(Apple ID 암호가 아님) */
public record ICloudLogin(String appleId, String appPassword) {

    // 기록(toString)에 앱 암호가 새지 않게
    @Override
    public String toString() {
        return "ICloudLogin(" + appleId + ")";
    }
}
