## 실행

레포 루트에서 한 번에 DB, Kafka, API 서버, 웹이 모두 뜹니다.

```bash
docker compose up -d --build   # 켜기 (처음이거나 코드가 바뀌었을 때 --build)
docker compose down            # 끄기 — 데이터는 남는다. -v를 붙이면 DB까지 지워지니 주의
```

브라우저에서 <http://localhost:3000> 으로 접속한다. 모든 포트는 로컬(127.0.0.1)에서만 열립니다.

| 컨테이너 | 내용 | 포트 |
|---|---|---|
| hubdash-web | 웹 클라이언트(nginx), `/api`는 API로 넘김 | 3000 |
| hubdash-api | Spring Boot API (`server/`) | 8080 |
| hubdash-db | PostgreSQL 16 | 5432 |
| hubdash-kafka | Kafka (raw → ETL 이벤트) | 9092 |

### 백업과 복원

데이터는 도커 볼륨(`server_hubdash-db-data`)에만 있습니다. `docker compose down -v`나 Docker Desktop 초기화 한 번이면
사라지니, 가끔 파일로 떠 듭니다.

```bash
mkdir -p ~/hubdash-backups
docker exec hubdash-db pg_dump -U hubdash -d hubdash -Fc > ~/hubdash-backups/hubdash-$(date +%Y%m%d).dump

# 복원: API를 멈추고 덮어쓴 뒤 다시 켠다
docker compose stop api
docker exec -i hubdash-db pg_restore -U hubdash -d hubdash --clean --if-exists --no-owner < ~/hubdash-backups/hubdash-YYYYMMDD.dump
docker compose start api
```

주간 통계는 매일 00:10과 **앱이 뜰 때** 지난주를 다시 집계됩니다.

개발 중에는 `docker compose up -d db kafka`만 띄우고 API는 `server/`에서 `./gradlew :api:bootRun`,
웹은 `client/web/`에서 `npm run dev`로 띄웁니다. 자세한 건 [`server/README.md`](server/README.md),
[`client/web/README.md`](client/web/README.md).

## 서버 아키텍쳐
<img width="3000" height="1800" alt="hubdash-architecture" src="https://github.com/user-attachments/assets/fedb45a9-68ff-4cc0-877c-371de295bb6e" />
