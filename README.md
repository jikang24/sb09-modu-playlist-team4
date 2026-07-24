# modu-playlist (mopl)

> "흩어진 콘텐츠를 하나의 플레이리스트로, 함께 보고 함께 이야기하세요!"

모두의 플리 - 콘텐츠(영화/드라마 등)를 모아 플레이리스트로 큐레이션하고, 다른 사용자와 함께 시청하며 소통할 수 있는 콘텐츠 플레이리스트 커뮤니티 서비스의 백엔드 서버입니다. - SB9기 4팀 고급프로젝트

![Coverage](https://raw.githubusercontent.com/jikang24/sb09-modu-playlist-team4/badges/.github/badges/coverage.svg)

## Table of Contents

1. [Core Features](#core-features)
2. [Technology Stack](#technology-stack)
3. [File Structure](#file-structure)
4. [Getting Started](#getting-started)
5. [API Documentation](#api-documentation)
6. [Team](#team)

---

## Core Features

**인증/인가**: 이메일 회원가입 및 로그인, JWT 기반 인증, Google/Kakao OAuth2 소셜 로그인

**콘텐츠**: TMDB 연동 콘텐츠 수집, OpenSearch 기반 콘텐츠 검색

**플레이리스트**: 플레이리스트 생성/수정/삭제, 콘텐츠 담기

**리뷰**: 콘텐츠에 대한 리뷰 작성/수정/삭제

**팔로우**: 사용자 팔로우/언팔로우

**함께 시청**: 실시간 시청 세션(watching session) 생성 및 참여

**채팅/DM**: 콘텐츠 채팅, 사용자 간 다이렉트 메시지

**알림**: 사용자 행동 기반 실시간 알림

**배치**: ShedLock 기반 분산 락으로 다중 인스턴스 환경에서 안전한 배치 작업 수행

---

## Technology Stack

**Backend**: Java 17, Spring Boot 3.5, Spring Data JPA, QueryDSL, Spring Security, Spring Batch, Spring Validation

**Database / Cache**: PostgreSQL, Redis

**Search**: OpenSearch

**Messaging**: Apache Kafka

**Auth**: JWT (jjwt), OAuth2 Client (Google, Kakao)

**External API**: TMDB API

**Storage**: AWS S3

**Docs**: springdoc-openapi (Swagger)

**Object Mapping**: MapStruct

**Distributed Lock**: ShedLock (Redis)

**Test**: JUnit5, Spring Security Test, Spring Batch Test, Testcontainers, Awaitility, H2, JaCoCo

**Build / Infra**: Gradle, Docker, Docker Compose, Nginx

---

## File Structure

```
src/main/java/com/mopl
├── domain
│   ├── auth              # 인증/인가, JWT, OAuth2
│   ├── user              # 사용자
│   ├── content           # 콘텐츠 (영화/드라마 등)
│   ├── playlist          # 플레이리스트
│   ├── review             # 리뷰
│   ├── follow             # 팔로우
│   ├── dm                 # 다이렉트 메시지
│   ├── contentchat        # 콘텐츠 채팅
│   ├── conversation       # 대화
│   ├── watchingsession    # 함께 시청 세션
│   ├── notification       # 알림
│   └── batch              # 배치 작업
├── global                 # 공통 설정, 예외 처리, 유틸리티
└── infra                  # 외부 연동 (S3, OpenSearch, Kafka, TMDB 등)
```

---

## Getting Started

### 요구 사항

- JDK 17
- Docker / Docker Compose

### 환경 변수 설정

`.env.dev` 파일을 참고하여 프로젝트 루트에 필요한 환경 변수 파일(`.env` 또는 `.env.dev`)을 구성합니다.

```
SPRING_PROFILES_ACTIVE=
SERVER_PORT=
FRONTEND_BASE_URL=

# DB
DB_HOST=
DB_PORT=
DB_NAME=
DB_USERNAME=
DB_PASSWORD=

# Redis
REDIS_HOST=
REDIS_PORT=

# Kafka
KAFKA_BOOTSTRAP_SERVERS=
KAFKA_API_KEY=
KAFKA_API_SECRET=

# OpenSearch
OPENSEARCH_URI=
OPENSEARCH_INITIAL_ADMIN_PASSWORD=

# JWT
JWT_SECRET=
JWT_ACCESS_TOKEN_EXPIRY_MS=
JWT_REFRESH_TOKEN_EXPIRY_MS=

# Mail
MAIL_USERNAME=
MAIL_PASSWORD=

# TMDB
TMDB_API_KEY=
TMDB_ACCESS_TOKEN=

# AWS S3
CLOUD_AWS_REGION_STATIC=
CLOUD_AWS_CREDENTIALS_ACCESS_KEY=
CLOUD_AWS_CREDENTIALS_SECRET_KEY=

# OAuth2
KAKAO_CLIENT_ID=
KAKAO_CLIENT_SECRET=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
```

### 로컬 실행 (Docker Compose)

```bash
docker compose up -d --build
```

Nginx(80), API 서버(내부 8080), PostgreSQL(5432), Redis(6379), Kafka(9092), OpenSearch(9200)가 함께 기동됩니다.

### 로컬 실행 (Gradle)

인프라(DB, Redis, Kafka, OpenSearch 등)만 별도로 띄운 뒤 애플리케이션을 직접 실행할 수 있습니다.

```bash
./gradlew bootRun
```

기본 프로파일은 `dev`이며, `local`/`test`/`prod` 프로파일은 `src/main/resources/application-{profile}.yml`에서 확인할 수 있습니다.

### 빌드

```bash
./gradlew build
```

### 테스트

```bash
./gradlew test
```

테스트 실행 후 JaCoCo 커버리지 리포트가 `build/reports/jacoco/test/html/index.html`에 생성됩니다.

---

## API Documentation

애플리케이션 실행 후 Swagger UI에서 API 명세를 확인할 수 있습니다.

```
http://localhost:8080/swagger-ui/index.html
```

---

## Team

SB9기 4팀 - 판타스틱 4

| 이름 | GitHub | 주요 담당 |
|------|--------|-----------|
| 강지원 | [@jikang24](https://github.com/jikang24) | 콘텐츠, 시청세션, Batch, 캐싱전략 구축 |
| 나은비 | [@nano-mm](https://github.com/nano-mm) | AWS, 플레이리스트, 알림, SSE, Kafka, OpenSearch |
| 박지은 | [@clover6559](https://github.com/clover6559) | 사용자, OAuth, 팔로우, 인증 및 보안 관리 |
| 전승현 | [@seunghyeonjeon57-dot](https://github.com/seunghyeonjeon57-dot) | AWS, WebSocket, DM, Redis, 인프라 구축 |
