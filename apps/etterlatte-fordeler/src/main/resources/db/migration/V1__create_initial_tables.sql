
CREATE TABLE fordelinger
(
    soeknad_id BIGINT PRIMARY KEY,
    fordeling TEXT NOT NULL,
    opprettet_ts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE kriterietreff
(
    soeknad_id BIGINT NOT NULL
        REFERENCES fordelinger (soeknad_id)
            ON DELETE CASCADE,
    kriterie TEXT NOT NULL

);