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

# docker 소켓을 마운트해 쓰므로(호스트 도커 데몬에 컨테이너를 띄우는 구조) Testcontainers가 띄운 컨테이너의 게시 포트는
# 이 컨테이너가 아니라 호스트에서 열린다. 기본 추정(브리지 게이트웨이 172.17.0.1)이 맞지 않는 환경(Docker Desktop 등)에서
# Ryuk에 접속하지 못해 테스트가 실패하므로 호스트 이름을 고정한다. 리눅스에서는 실행 시 --add-host=host.docker.internal:host-gateway가 필요하다.
ENV TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal

USER jenkins
# workflow-aggregator=Pipeline, github=웹훅 트리거(githubPush), credentials-binding=withCredentials
RUN jenkins-plugin-cli --plugins workflow-aggregator git github credentials-binding timestamper junit
