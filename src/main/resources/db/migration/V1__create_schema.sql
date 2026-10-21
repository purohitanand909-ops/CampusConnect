-- =============================================================
-- V1__create_schema.sql
-- Initial schema for CampusConnect
-- Tables: users, events, registrations
-- =============================================================

CREATE TABLE users (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)    NOT NULL,
    email       VARCHAR(150)    NOT NULL,
    role        ENUM('STUDENT','ORGANIZER') NOT NULL DEFAULT 'STUDENT',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------

CREATE TABLE events (
    id                      BIGINT          NOT NULL AUTO_INCREMENT,
    title                   VARCHAR(200)    NOT NULL,
    description             TEXT,
    category                VARCHAR(80)     NOT NULL,
    location                VARCHAR(200)    NOT NULL,
    start_time              DATETIME        NOT NULL,
    registration_deadline   DATETIME        NOT NULL,
    capacity                INT             NOT NULL,
    status                  ENUM('UPCOMING','CANCELLED') NOT NULL DEFAULT 'UPCOMING',
    organizer_id            BIGINT          NOT NULL,
    created_at              DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_events_organizer FOREIGN KEY (organizer_id) REFERENCES users (id),
    CONSTRAINT chk_events_capacity CHECK (capacity > 0),
    CONSTRAINT chk_events_deadline CHECK (registration_deadline < start_time),

    INDEX idx_events_category (category),
    INDEX idx_events_status   (status),
    INDEX idx_events_start    (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------

CREATE TABLE registrations (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    student_id          BIGINT      NOT NULL,
    event_id            BIGINT      NOT NULL,
    status              ENUM('CONFIRMED','WAITLISTED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    waitlist_position   INT         NULL COMMENT 'NULL for CONFIRMED; 1-based position for WAITLISTED',
    registered_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_reg_student FOREIGN KEY (student_id) REFERENCES users (id),
    CONSTRAINT fk_reg_event   FOREIGN KEY (event_id)   REFERENCES events (id),

    -- A student may have at most one active (non-cancelled) registration per event
    CONSTRAINT uq_reg_active UNIQUE (student_id, event_id),

    INDEX idx_reg_event   (event_id),
    INDEX idx_reg_student (student_id),
    INDEX idx_reg_status  (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
