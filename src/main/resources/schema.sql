-- ================================================================
-- 모두의 플리 (MoPl) Schema
-- 팀원 ERD 기준으로 작성
-- 표기 규칙:
--   FK  : 모듈 내부 외래키 (DB 제약 있음)
--   REF : 모듈 간 ID 참조 (DB 제약 없음 — 값만 저장)
-- ================================================================


-- ================================================================
-- [user 모듈]
-- ================================================================

CREATE TABLE IF NOT EXISTS users (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(100)    NOT NULL UNIQUE,
    password            VARCHAR(255),                       -- nullable: OAuth 전용 계정
    name                VARCHAR(50)     NOT NULL,
    profile_image_url   VARCHAR(500),
    role                VARCHAR(10)     NOT NULL DEFAULT 'USER'
                                        CHECK (role IN ('USER', 'ADMIN')),
    locked              BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS social_accounts (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 내부 FK] user 모듈 → user 모듈
    user_id             UUID            NOT NULL,
    provider            VARCHAR(10)     NOT NULL
                                        CHECK (provider IN ('GOOGLE', 'KAKAO')),
    provider_user_id    VARCHAR(100)    NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    UNIQUE (provider, provider_user_id),

    CONSTRAINT fk_social_accounts_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 내부 FK] user 모듈 → user 모듈
    user_id             UUID            NOT NULL,
    temporary_password  VARCHAR(255)    NOT NULL,
    expires_at          TIMESTAMP       NOT NULL,           -- 3분 만료, 앱에서 체크
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    used                BOOLEAN         NOT NULL default false,

    CONSTRAINT fk_password_reset_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);


-- ================================================================
-- [content 모듈]
-- ================================================================

CREATE TABLE IF NOT EXISTS contents (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    type            VARCHAR(10)     NOT NULL
                                    CHECK (type IN ('MOVIE', 'TV_SERIES', 'SPORT')),
    external_id     VARCHAR(100)    NOT NULL,               -- TMDB ID 또는 SportsDB ID
    title           VARCHAR(200)    NOT NULL,
    description     TEXT,
    thumbnail_url   VARCHAR(500),
    average_rating  DECIMAL(3, 2)   NOT NULL DEFAULT 0.0,  -- reviews 집계 캐시값
    review_count    INT             NOT NULL DEFAULT 0,     -- reviews 집계 캐시값
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (type, external_id)                             -- 중복 수집 방지
);

CREATE TABLE IF NOT EXISTS content_tags (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 내부 FK] content 모듈 → content 모듈
    content_id      UUID            NOT NULL,
    tag             VARCHAR(50)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    UNIQUE (content_id, tag),                              -- 같은 태그 중복 방지

    CONSTRAINT fk_content_tags_content
        FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE
);


-- ================================================================
-- [playlist 모듈]
-- ================================================================

CREATE TABLE IF NOT EXISTS playlists (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 간 REF] user 모듈의 users.id 참조 — FK 없음
    owner_id        UUID            NOT NULL,
    title           VARCHAR(100)    NOT NULL,
    description     TEXT,
    is_public       BOOLEAN         NOT NULL DEFAULT TRUE,
    deleted         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS playlist_contents (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 내부 FK] playlist 모듈 → playlist 모듈
    playlist_id     UUID            NOT NULL,
    -- [모듈 간 REF] content 모듈의 contents.id 참조 — FK 없음
    content_id      UUID            NOT NULL,
    position        INT             NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    UNIQUE (playlist_id, content_id),                      -- 중복 추가 방지

    CONSTRAINT fk_playlist_contents_playlist
        FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS playlist_subscriptions (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 내부 FK] playlist 모듈 → playlist 모듈
    playlist_id     UUID            NOT NULL,
    -- [모듈 간 REF] user 모듈의 users.id 참조 — FK 없음
    subscriber_id   UUID            NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    UNIQUE (playlist_id, subscriber_id),                   -- 중복 구독 방지

    CONSTRAINT fk_playlist_subscriptions_playlist
        FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE
);


-- ================================================================
-- [review 모듈]
-- ================================================================

CREATE TABLE IF NOT EXISTS reviews (
    id                          UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 간 REF] user 모듈의 users.id 참조 — FK 없음
    user_id                     UUID            NOT NULL,
    -- [모듈 간 REF] content 모듈의 contents.id 참조 — FK 없음
    content_id                  UUID            NOT NULL,
    rating                      DECIMAL(2, 1)   NOT NULL
                                                 CHECK (rating >= 0.5 AND rating <= 5.0),
    text                        TEXT,
    -- 작성 시점 user 스냅샷 (목록 조회 시 user 모듈 재조회 방지)
    author_name                 VARCHAR(50)     NOT NULL,
    author_profile_image_url   VARCHAR(500),
    created_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP       NOT NULL DEFAULT NOW(),

    UNIQUE (user_id, content_id)                            -- 한 유저가 같은 콘텐츠에 리뷰 1개
);


-- ================================================================
-- [follow 모듈]
-- ================================================================

CREATE TABLE IF NOT EXISTS follows (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 간 REF] user 모듈의 users.id 참조 — FK 없음 (2개)
    follower_id     UUID            NOT NULL,               -- 팔로우 하는 사람
    followee_id     UUID            NOT NULL,               -- 팔로우 받는 사람
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    UNIQUE (follower_id, followee_id),                     -- 중복 팔로우 방지
    CHECK (follower_id != followee_id)                     -- 자기 자신 팔로우 방지
);


-- ================================================================
-- [chat 모듈]
-- ================================================================

CREATE TABLE IF NOT EXISTS conversations (
    id                  UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 간 REF] user 모듈의 users.id 참조 — FK 없음 (2개)
    participant1_id     UUID    NOT NULL,
    participant2_id     UUID    NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    UNIQUE (participant1_id, participant2_id),
    CHECK (participant1_id < participant2_id)               -- 항상 작은 UUID가 participant1
                                                           -- (순서 무관 중복 방지)
);

CREATE TABLE IF NOT EXISTS direct_messages (
    id                  UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 내부 FK] chat 모듈 → chat 모듈
    conversation_id     UUID        NOT NULL,
    -- [모듈 간 REF] user 모듈의 users.id 참조 — FK 없음
    sender_id           UUID        NOT NULL,
    receiver_id         UUID        NOT NULL,
    content             TEXT        NOT NULL,
    is_read             BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_direct_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
);


-- ================================================================
-- [notification 모듈]
-- ================================================================

CREATE TABLE IF NOT EXISTS notifications (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    -- [모듈 간 REF] user 모듈의 users.id 참조 — FK 없음
    -- 알림에 필요한 텍스트는 이벤트 발행 시 미리 담아서 저장
    -- → user/content 모듈 재조회 불필요
    receiver_id     UUID        NOT NULL,
    type            VARCHAR(50) NOT NULL,                   -- ROLE_CHANGED, PLAYLIST_UPDATED 등
    title           VARCHAR(100) NOT NULL,
    content         TEXT        NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- ================================================================
-- watching_session 은 DB 테이블 없음
-- Redis로 관리: watching:{contentId} → Set<userId>
-- 이유: 실시간 입/퇴장이 잦아 DB 부하 큼
--       요구사항에 "DB 저장 안 함" 명시
-- ================================================================


-- ================================================================
-- 인덱스
-- ================================================================

-- users
CREATE INDEX IF NOT EXISTS idx_users_email
    ON users (email);

-- social_accounts
CREATE INDEX IF NOT EXISTS idx_social_accounts_user_id
    ON social_accounts (user_id);

-- password_reset_tokens
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_id
    ON password_reset_tokens (user_id);

-- contents
CREATE INDEX IF NOT EXISTS idx_contents_type
    ON contents (type);
CREATE INDEX IF NOT EXISTS idx_contents_external_id
    ON contents (type, external_id);

-- playlists
CREATE INDEX IF NOT EXISTS idx_playlists_owner_id
    ON playlists (owner_id);                               -- 내 플레이리스트 목록 조회

-- playlist_contents
CREATE INDEX IF NOT EXISTS idx_playlist_contents_playlist_id
    ON playlist_contents (playlist_id);
CREATE INDEX IF NOT EXISTS idx_playlist_contents_content_id
    ON playlist_contents (content_id);

-- playlist_subscriptions
CREATE INDEX IF NOT EXISTS idx_playlist_subscriptions_subscriber_id
    ON playlist_subscriptions (subscriber_id);             -- 내가 구독한 플리 목록

-- reviews
CREATE INDEX IF NOT EXISTS idx_reviews_content_id
    ON reviews (content_id);                               -- 콘텐츠별 리뷰 목록
CREATE INDEX IF NOT EXISTS idx_reviews_user_id
    ON reviews (user_id);                                  -- 내가 쓴 리뷰 목록

-- follows
CREATE INDEX IF NOT EXISTS idx_follows_follower_id
    ON follows (follower_id);                              -- 내가 팔로우한 목록
CREATE INDEX IF NOT EXISTS idx_follows_followee_id
    ON follows (followee_id);                              -- 나를 팔로우한 목록

-- conversations
CREATE INDEX IF NOT EXISTS idx_conversations_participant1_id
    ON conversations (participant1_id);
CREATE INDEX IF NOT EXISTS idx_conversations_participant2_id
    ON conversations (participant2_id);

-- direct_messages
CREATE INDEX IF NOT EXISTS idx_direct_messages_conversation_id
    ON direct_messages (conversation_id);
CREATE INDEX IF NOT EXISTS idx_direct_messages_receiver_id
    ON direct_messages (receiver_id, is_read);             -- 읽지 않은 DM 조회

-- notifications
CREATE INDEX IF NOT EXISTS idx_notifications_receiver_id
    ON notifications (receiver_id);                        -- 내 알림 목록
CREATE INDEX IF NOT EXISTS idx_notifications_receiver_is_read
    ON notifications (receiver_id, is_read);               -- 읽지 않은 알림 조회

-- outbox
CREATE TABLE IF NOT EXISTS outbox_event (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,

    event_type VARCHAR(100) NOT NULL,

    payload TEXT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    retry_count INT NOT NULL DEFAULT 0,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    published_at TIMESTAMP NULL
    );

CREATE INDEX idx_outbox_status
    ON outbox_event(status, created_at);