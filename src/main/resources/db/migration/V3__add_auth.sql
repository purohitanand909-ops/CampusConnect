-- =============================================================
-- V3__add_auth.sql
-- Adds password_hash to users for Spring Security authentication.
-- All seed users are given the BCrypt hash of "password"
-- (cost=10). Change these via your application after first login.
--
-- BCrypt("password", cost=10) =
--   $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
-- =============================================================

-- Step 1: add column as nullable so existing rows are not broken
ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(255) NULL
        AFTER email;

-- Step 2: set a known BCrypt hash for every seed user
--         (all use password = "password" for local development)
UPDATE users
SET password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';

-- Step 3: enforce NOT NULL now that every row has a value
ALTER TABLE users
    MODIFY COLUMN password_hash VARCHAR(255) NOT NULL;
