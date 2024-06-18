CREATE TABLE avkortet_ytelse_periode
(
    id          UUID PRIMARY KEY         DEFAULT (uuid_generate_v4()),
    vedtakId    BIGINT                                                      NOT NULL REFERENCES vedtak (id),
    opprettet   TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    datoFom     DATE                                                        NOT NULL,
    datoTom     DATE,
    type        TEXT                                                        NOT NULL,
    ytelseFoer  INT                                                         NOT NULL,
    ytelseEtter INT                                                         NOT NULL
);

CREATE INDEX ON avkortet_ytelse_periode (vedtakId);
