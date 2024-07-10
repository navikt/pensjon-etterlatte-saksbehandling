CREATE TABLE samordning_manuell
(
    id            UUID PRIMARY KEY         DEFAULT (uuid_generate_v4()),
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    opprettet_av  TEXT                                                        NOT NULL,
    vedtakId      BIGINT                                                      NOT NULL REFERENCES vedtak (id),
    samId         BIGINT                                                      NOT NULL,
    refusjonskrav BOOLEAN                                                     NOT NULL,
    kommentar     TEXT                                                        NOT NULL
);

CREATE INDEX ON samordning_manuell (vedtakId);
