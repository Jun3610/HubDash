package com.junyoung.dashboard.global.icloud;

/** iCloud(CalDAV)를 쓸 수 없을 때 — 사용자에게 보여 줄 문구. 앱 암호를 넣지 않는다 */
public class ICloudException extends RuntimeException {
    public ICloudException(String message) {
        super(message);
    }
}
