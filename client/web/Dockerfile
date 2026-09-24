# 웹 클라이언트 이미지 — 빌드한 정적 파일을 nginx로 서빙하고 /api는 API 컨테이너로 넘긴다.
# 실행은 레포 루트의 docker-compose.yml에서 한다. 화면을 고치는 중에는 이 이미지 대신 `npm run dev`를 쓴다.

FROM node:22-alpine AS build
WORKDIR /app
# 의존성 레이어를 소스와 분리해, 소스만 바뀌면 npm ci를 다시 하지 않는다
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
