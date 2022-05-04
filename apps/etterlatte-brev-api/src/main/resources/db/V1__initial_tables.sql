CREATE TABLE brev
(
    id BIGSERIAL
        CONSTRAINT brev_pk
            PRIMARY KEY,
    vedtak_id BIGINT NOT NULL,
    pdf BYTEA NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL
);
