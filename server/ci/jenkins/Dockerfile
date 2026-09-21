# HubDash 파이프라인용 Jenkins 이미지 — JDK 17(빌드/테스트) + docker CLI(이미지 빌드/푸시, Testcontainers) + kubectl(배포).
# Jenkins는 k3s 클러스터 밖(같은 서버의 Docker 컨테이너)에서 돌린다. 실행 방법은 ci/README.md 참고.
FROM jenkins/jenkins:lts-jdk17

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg \
    && install -m 0755 -d /etc/apt/keyrings \
    && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
    && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends docker-ce-cli \
    && rm -rf /var/lib/apt/lists/*

# kubectl은 k3s 서버 버전과 ±1 마이너 이내여야 한다. 아키텍처(amd64/arm64)에 맞는 바이너리를 받는다.
ARG KUBECTL_VERSION=v1.34.1
RUN curl -fsSL -o /usr/local/bin/kubectl "https://dl.k8s.io/release/${KUBECTL_VERSION}/bin/linux/$(dpkg --print-architecture)/kubectl" \
    && chmod +x /usr/local/bin/kubectl

USER jenkins
# workflow-aggregator=Pipeline, github=웹훅 트리거(githubPush), credentials-binding=withCredentials
RUN jenkins-plugin-cli --plugins workflow-aggregator git github credentials-binding timestamper junit
