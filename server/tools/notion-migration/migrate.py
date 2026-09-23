"""노션 기존 데이터를 로컬 HubDash로 한 번 복사해 넣는 일회성 이관 스크립트 (이슈 #94).

노션과 동기화하지 않는다. 입력은 노션에서 읽어 변환해 둔 JSON(data/notion-export.json, 개인 데이터라 커밋하지 않음)이고,
DB에 직접 INSERT하지 않고 서버 API로 넣는다 — 일정/메모/허브 링크는 raw 엔드포인트로 넣어 평소 입력과 같은
Kafka raw → ETL 경로를 타게 하고, ID가 바로 필요한 부모 엔티티(카테고리, 주제, 학기, 과목)는 일반 CRUD로 넣는다.

사용법:
    python3 migrate.py data/notion-export.json            # 대상 테이블이 비어 있을 때만 실행
    HUBDASH_BASE_URL=... HUBDASH_API_KEY=... python3 migrate.py ...
"""
import json
import os
import sys
import time
import urllib.error
import urllib.request

BASE_URL = os.environ.get("HUBDASH_BASE_URL", "http://localhost:8080")
API_KEY = os.environ.get("HUBDASH_API_KEY", "dev-local-key")

# 이관 대상 목록 API — 하나라도 데이터가 있으면 중복 이관을 막기 위해 멈춘다.
# 허브 링크와 과목은 부모(카테고리, 학기) 없이 존재할 수 없어 부모만 확인하면 된다.
TARGET_LISTS = [
    "/api/schedule/events",
    "/api/memo/memos",
    "/api/hub/categories",
    "/api/study/topics",
    "/api/pknu/semesters",
]


def call(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(BASE_URL + path, data=data, method=method)
    req.add_header("X-API-KEY", API_KEY)
    req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as res:
            return json.loads(res.read())["data"]
    except urllib.error.HTTPError as e:
        raise RuntimeError(f"{method} {path} -> {e.code}: {e.read().decode()}") from None


def total(path, query=""):
    return call("GET", f"{path}?size=1{query}")["totalElements"]


def child_total(parent_path, child_path, parent_param):
    """부모 ID가 필수인 하위 목록(허브 링크, 과목)은 부모마다 세어 합친다."""
    parents = call("GET", f"{parent_path}?size=1000")["content"]
    return sum(total(child_path, f"&{parent_param}={p['id']}") for p in parents)


def wait_raw(kind, path, ids, timeout_s=120):
    """raw 행이 ETL을 거쳐 PENDING에서 벗어날 때까지 기다린 뒤 실패 건을 돌려준다."""
    deadline = time.time() + timeout_s
    pending = set(ids)
    failed = []
    while pending and time.time() < deadline:
        for raw_id in list(pending):
            raw = call("GET", f"{path}/{raw_id}")
            if raw["status"] != "PENDING":
                pending.discard(raw_id)
                if raw["status"] != "PROCESSED":
                    failed.append((raw_id, raw.get("failureReason")))
        if pending:
            time.sleep(1)
    print(f"  {kind}: 처리 {len(ids) - len(pending) - len(failed)}, 실패 {len(failed)}, 미처리 {len(pending)}")
    return failed, pending


def main():
    export = json.load(open(sys.argv[1]))

    existing = {p: total(p) for p in TARGET_LISTS}
    if any(existing.values()):
        sys.exit(f"대상 테이블에 이미 데이터가 있어 중단합니다: {existing}")

    print("프로필")
    profile = export["profile"]
    call("PUT", "/api/user/profile", {"displayName": profile["displayName"], "email": None, "bio": profile["bio"]})

    print("허브 카테고리/링크")
    link_raw_ids = []
    for category in export["hub"]:
        category_id = call("POST", "/api/hub/categories",
                           {"name": category["name"], "description": category["description"]})["id"]
        for link in category["links"]:
            link_raw_ids.append(call("POST", "/api/hub/raw/links", {
                "categoryIdRaw": str(category_id),
                "titleRaw": link["title"],
                "urlRaw": link["url"],
                "description": link["description"],
            })["id"])

    print("공부 주제")
    for topic in export["studyTopics"]:
        call("POST", "/api/study/topics", topic)

    print("학기/과목")
    for semester in export["semesters"]:
        semester_id = call("POST", "/api/pknu/semesters", {
            "name": semester["name"], "startDate": semester["startDate"], "endDate": semester["endDate"],
        })["id"]
        for course in semester["courses"]:
            call("POST", "/api/pknu/courses", {
                "semesterId": semester_id, "name": course["name"], "professor": None, "credit": course["credit"],
            })

    print("메모")
    memo_raw_ids = [call("POST", "/api/memo/raw/memos", {
        "titleRaw": memo["title"], "contentRaw": memo["content"], "tags": memo["tags"],
    })["id"] for memo in export["memos"]]

    print("일정")
    event_raw_ids = [call("POST", "/api/schedule/raw/events", {
        "titleRaw": event["title"],
        "startAtRaw": event["startAt"],
        "endAtRaw": event["endAt"],
        "location": event["location"],
        "description": None,
        "allDayRaw": str(event["allDay"]).lower(),
    })["id"] for event in export["events"]]

    print("ETL 처리 대기")
    problems = []
    problems += wait_raw("허브 링크", "/api/hub/raw/links", link_raw_ids)[0]
    problems += wait_raw("메모", "/api/memo/raw/memos", memo_raw_ids)[0]
    problems += wait_raw("일정", "/api/schedule/raw/events", event_raw_ids)[0]
    for raw_id, reason in problems:
        print(f"  실패 raw {raw_id}: {reason}")

    checks = [
        ("일정", len(export["events"]), total("/api/schedule/events")),
        ("메모", len(export["memos"]), total("/api/memo/memos")),
        ("허브 카테고리", len(export["hub"]), total("/api/hub/categories")),
        ("허브 링크", sum(len(c["links"]) for c in export["hub"]),
         child_total("/api/hub/categories", "/api/hub/links", "categoryId")),
        ("공부 주제", len(export["studyTopics"]), total("/api/study/topics")),
        ("학기", len(export["semesters"]), total("/api/pknu/semesters")),
        ("과목", sum(len(s["courses"]) for s in export["semesters"]),
         child_total("/api/pknu/semesters", "/api/pknu/courses", "semesterId")),
    ]
    print("검증 (기대 / 실제)")
    mismatch = False
    for name, want, got in checks:
        mismatch |= got != want
        print(f"  {name}: {want} / {got}{'' if got == want else '  <-- 불일치'}")
    if mismatch:
        sys.exit(1)


if __name__ == "__main__":
    main()
