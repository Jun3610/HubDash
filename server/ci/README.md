# CI/CD (Jenkins → GHCR → k3s)

`Jenkinsfile`이 테스트 → 이미지 빌드 → GHCR push → k3s 배포 → 롤아웃 확인(실패 시 `rollout undo`)을 수행합니다.
Jenkins는 **k3s와 같은 서버의 Docker 컨테이너**로 운영합니다(클러스터 밖이라 클러스터 장애 시에도 CI/CD 유지).

## 검증 범위 (이슈 #82)

일회용 환경(Jenkins 컨테이너 + 로컬 레지스트리 + 일회용 k3s)에서 파이프라인을 **실제로 끝까지 실행**해 확인했습니다.
GHCR과 실제 서버에서는 실행해 보지 못했습니다 — 그 차이는 아래 "서버에서 처음 실행할 때"에 정리했습니다.

| 확인한 것 | 확인하지 못한 것 |
|---|---|
| 선언형 린터 통과(잘못된 파일은 거부하는 것도 확인) | 실제 GHCR push와 그 인증(PAT) |
| 테스트(Testcontainers 포함) → 이미지 빌드 → push → 배포 → 롤아웃 성공 | 실제 서버의 k3s/스토리지/도메인 |
| 갱신 배포(이미 떠 있는 클러스터에 새 태그) | GitHub 웹훅(`githubPush()`) 트리거 |
| 롤아웃 실패 시 `rollout undo`로 직전 이미지 복구 | 비공개 GHCR 패키지의 `imagePullSecrets` |
| 로그에 토큰/kubeconfig/시크릿 값이 노출되지 않음(마스킹) | |

## 서버 구성 순서

### 1) k3s 설치와 앱 시크릿

```bash
curl -sfL https://get.k3s.io | sh -                # k3s 설치
sudo cat /etc/rancher/k3s/k3s.yaml                  # kubeconfig (아래 3단계에서 Jenkins에 등록)

sudo k3s kubectl create namespace hubdash
sudo k3s kubectl -n hubdash create secret generic hubdash-secrets \
  --from-literal=API_KEY="$(openssl rand -hex 24)" \
  --from-literal=DB_PASSWORD="$(openssl rand -hex 24)"
```

`Deploy` 단계는 이 시크릿이 없으면 명확한 메시지와 함께 실패합니다.

### 2) Jenkins 컨테이너

```bash
docker build -t hubdash-jenkins server/ci/jenkins

docker run -d --name jenkins --restart unless-stopped \
  -p 8080:8080 \
  --add-host=host.docker.internal:host-gateway \
  --group-add "$(stat -c %g /var/run/docker.sock)" \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  hubdash-jenkins
```

- `--add-host`: 이미지가 `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`을 설정해 두었으므로, 리눅스에서는 이 옵션이 있어야 이름이 풀립니다. (없으면 Testcontainers가 Ryuk에 접속하지 못해 Flyway 테스트가 실패합니다.)
- `--group-add`: Jenkins 사용자가 docker 소켓을 쓰도록 합니다.
- **docker 소켓을 마운트한 컨테이너는 사실상 호스트 root 권한**입니다. 반드시 초기 설정에서 관리자 계정을 만들고 익명 접근을 끄세요(검증 환경에서는 셋업 마법사를 껐지만 운영에서는 절대 그렇게 하지 않습니다).
- 초기 관리자 비밀번호: `docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword`

### 3) 자격증명 등록 (Manage Jenkins → Credentials)

| ID | 종류 | 내용 |
|---|---|---|
| `ghcr-credentials` | Username with password | GitHub 사용자명 + `write:packages` 권한의 PAT |
| `k3s-kubeconfig` | Secret file | 1)의 kubeconfig. Jenkins는 컨테이너라 `server: https://127.0.0.1:6443`이 자기 자신을 가리키므로, 서버의 실제 IP/호스트명으로 바꾸고 k3s를 `--tls-san <그 주소>`로 설치/재설정해야 합니다 |

### 4) 잡 생성

Pipeline → Definition **Pipeline script from SCM** → Git(저장소 URL, 비공개면 자격증명) → Branch `*/main` → Script Path `server/Jenkinsfile`.
파라미터 `IMAGE_REPO`의 기본값은 `ghcr.io/jun3610/hubdash-api`입니다(GHCR 소유자 이름은 **소문자**).

### 5) GitHub 웹훅

저장소 Settings → Webhooks → Payload URL `http://<Jenkins 주소>:8080/github-webhook/`, 이벤트 `push`.
(Jenkinsfile의 `githubPush()` 트리거가 받습니다. 웹훅으로 접근하려면 Jenkins가 외부에서 접근 가능해야 합니다.)

## 서버에서 처음 실행할 때 알아둘 것

- **첫 배포는 느립니다.** 새 클러스터는 `postgres`/`kafka` 이미지를 처음 받아야 해서(검증 환경에서 postgres만 5분 가까이) 롤아웃 제한을 600초로 잡았습니다.
- **GHCR 패키지가 비공개라면** k3s가 이미지를 받을 수 있게 pull 시크릿이 필요합니다(`k8s/app.yaml`에 `imagePullSecrets: ghcr-pull`이 선택 사항으로 들어 있음). 첫 push 뒤에 패키지를 공개로 바꾸거나 아래처럼 만드세요.
  ```bash
  kubectl -n hubdash create secret docker-registry ghcr-pull \
    --docker-server=ghcr.io --docker-username=<사용자> --docker-password=<read:packages PAT>
  ```
- **`Recreate` 배포라 롤아웃 실패 시 되돌리기 전까지 다운타임이 있습니다.** 앱이 replica 1 + 내부 스케줄러 구조이기 때문입니다(`k8s/README.md`).
- **테스트 단계는 Gradle이 입력 변경이 없다고 판단하면 `UP-TO-DATE`로 건너뜁니다.** Jenkinsfile/k8s만 바뀐 커밋에서는 테스트가 3초 만에 끝납니다(같은 워크스페이스를 재사용하는 경우).
- Jenkins 워크스페이스에 Gradle 캐시가 쌓입니다. `jenkins_home` 볼륨의 디스크 사용량을 가끔 확인하세요.
