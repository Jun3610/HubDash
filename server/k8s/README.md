# HubDash 배포 매니페스트 (k3s)

`kubectl apply -k`로 배포하는 kustomize 구성입니다. 일회용 k3s 컨테이너에 실제로 배포해 검증했습니다(이슈 #77).

| 파일 | 내용 |
|---|---|
| `namespace.yaml` | `hubdash` 네임스페이스 |
| `postgres.yaml` | Postgres 16 StatefulSet(PVC 5Gi) + 헤드리스 Service |
| `kafka.yaml` | Kafka 3.9(KRaft 단일 노드) StatefulSet(PVC 5Gi) + 헤드리스 Service |
| `app.yaml` | 앱 ConfigMap, Deployment(DB 대기 initContainer 포함), Service, Ingress(Traefik) |
| `secret.example.yaml` | 시크릿 **예제**(kustomization에 포함되지 않음) |

## 배포

```bash
# 1) 시크릿은 저장소에 두지 않고 클러스터에 직접 만든다
kubectl create namespace hubdash
kubectl -n hubdash create secret generic hubdash-secrets \
  --from-literal=API_KEY="$(openssl rand -hex 24)" \
  --from-literal=DB_PASSWORD="$(openssl rand -hex 24)"

# 2) 이미지와 호스트 지정 (이미지는 저장소 루트의 Dockerfile로 빌드)
cd k8s
kustomize edit set image hubdash-api=<레지스트리>/hubdash-api:<태그>   # 또는 kubectl kustomize로 확인
# app.yaml의 Ingress host(hubdash.example.com)를 실제 도메인으로 바꾼다

# 3) 배포
kubectl apply -k .
kubectl -n hubdash get pods
```

앱 이미지는 `prod` 프로파일로 뜨므로 `API_KEY`/`DB_PASSWORD` 시크릿이 없으면 파드가 기동하지 못합니다(개발용 기본값으로 뜨지 않음).

## 설계 결정과 제약

- **앱은 replica 1 + `Recreate`**: 앱 내부 스케줄러가 주간 집계 배치를 돌립니다. 여러 인스턴스가 같은 주를 동시에 집계하면 유니크 제약 충돌 위험이 있으므로 늘리지 마세요. 별도 CronJob은 만들지 않았습니다(앱 스케줄러와 중복).
- **DB/Kafka는 클러스터 안 StatefulSet**: 관리형으로 옮기려면 `postgres.yaml`/`kafka.yaml`을 `kustomization.yaml`에서 빼고 `hubdash-config`의 `DB_HOST`/`KAFKA_BOOTSTRAP`만 바꾸면 됩니다.
- **헬스체크는 TCP**: 앱에 actuator가 없어 HTTP 헬스 엔드포인트가 없습니다.
- **Kafka advertised 주소는 파드 DNS(`kafka-0.kafka...`)로 고정**하고 헤드리스 Service에 `publishNotReadyAddresses`를 켰습니다. Service를 거쳐 자기 자신에 접속하는 구조는 준비 전에 엔드포인트가 없어 쿼럼이 형성되지 못합니다.
- **앱 파드의 `wait-for-postgres` initContainer**: 서버 재부팅처럼 모든 파드가 동시에 뜰 때 앱이 DB보다 먼저 떠서 Flyway가 실패하고 CrashLoopBackOff로 재시작하는 것을 막습니다.

## 아직 없는 것

- **Postgres 백업**: PVC에만 있으므로 노드 디스크가 사라지면 데이터도 사라집니다. 백업 CronJob이 필요합니다.
- **TLS**: 도메인과 인증서 방식(cert-manager 등)이 정해지면 Ingress에 `tls`를 추가합니다.
- k3s는 기본 `local-path` 스토리지를 쓰므로 PVC가 특정 노드에 묶입니다(단일 노드 전제).
