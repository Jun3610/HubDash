# 로컬 실행 (Docker Compose)

compose 파일은 **레포 루트**의 `docker-compose.yml` 하나다(이슈 #126). 루트에서 `docker compose up -d --build`
한 번으로 DB, Kafka, API, 웹이 모두 뜬다. 아래 명령은 전부 레포 루트에서 실행한다.

## 빠르게 시작하기

```bash
docker compose up -d --build
curl -H "X-API-KEY: dev-local-key" http://127.0.0.1:8080/api/study/topics
```

웹은 `127.0.0.1:3000`, API는 `127.0.0.1:8080`, Postgres는 `127.0.0.1:5432`, Kafka는 `127.0.0.1:9092`에서만 열린다 —
같은 네트워크의 다른 기기에서는 접근할 수 없다.

## 앱을 IDE/bootRun으로 개발할 때

컨테이너 앱 대신 IDE에서 디버깅하며 개발하려면 DB/Kafka만 띄우고 앱은 `bootRun`으로 따로 실행한다.

```bash
docker compose up -d db kafka      # 레포 루트에서
cd server && ./gradlew :api:bootRun
```

이때 앱은 `application.properties`의 기본값(`localhost:5432`, `localhost:9092`)을 그대로 쓴다.

## 구성 요약

- **db**: `postgres:16`. `pg_isready` 헬스체크 통과 후에야 앱이 기동한다.
- **kafka**: `apache/kafka:3.9.0`. 리스너가 둘이다 — `PLAINTEXT`(`localhost:9092`, 호스트에서 `bootRun`으로
  붙을 때)와 `INTERNAL`(`kafka:19092`, 같은 compose 네트워크의 `api` 컨테이너가 붙을 때). 컨테이너 안에서
  `localhost`는 자기 자신이라 리스너 하나로는 둘 다 처리할 수 없다.
- **api**: `server/Dockerfile`로 빌드(`prod` 프로파일). `db`/`kafka`가 healthy가 된 뒤에만 기동한다.
  이미지는 시크릿에 기본값이 없어서(`API_KEY`, `DB_PASSWORD` 미설정 시 기동 실패) compose가 로컬 전용 기본값
  (`dev-local-key` / `hubdash`)을 제공한다 — 바꾸려면 레포 루트 `.env`에 `API_KEY=...`, `DB_PASSWORD=...`를 적는다.
- **web**: `client/web/Dockerfile`로 빌드(정적 파일을 nginx로 서빙). `/api/`는 `api:8080`으로 프록시해
  웹과 API가 같은 주소(`127.0.0.1:3000`)라 CORS 설정이 필요 없다.
- Postgres 데이터와 Kafka 로그/오프셋은 named volume(`server_hubdash-db-data`, `server_hubdash-kafka-data` —
  compose가 `server/`에 있던 때 만든 이름을 그대로 이어 씀)에 남아
  `docker compose down`(볼륨 삭제 없이) 후 다시 `up`해도 유지된다. 완전히 초기화하려면
  `docker compose down -v`.

## 검증한 것 (2026-09-22)

`down -v`로 볼륨까지 지운 뒤 처음부터 새로 띄워 확인했다.

- `docker compose up -d --build`로 DB → Kafka → 앱 순서로 기동, Flyway 마이그레이션(24개) 성공,
  Tomcat 정상 기동
- API 골든 패스(`POST /api/study/topics` 201, `GET` 200)와 실패 경로(API 키 없음/오답 401,
  필수값 누락 400) 응답 확인
- Kafka를 거치는 ETL 경로(`POST /api/schedule/raw/events` → Kafka 발행 → 컨슈머가 소비해 정규화된
  `Event` 생성)를 골든 패스(`PROCESSED`)와 실패 경로(날짜 파싱 실패 → `FAILED`, 재시도 없이 정상 종료)
  양쪽 다 확인
- `docker compose down`(볼륨 유지) 후 컨테이너를 완전히 새로 만들어 재기동 — Postgres 데이터
  (study_topic, raw_event, 정규화된 event)와 Kafka 컨슈머 오프셋(`kafka-consumer-groups.sh`로
  `CURRENT-OFFSET == LOG-END-OFFSET`, `LAG 0` 확인, 재처리로 인한 중복 없음) 모두 유지되는 것을 확인
