CREATE TABLE simulering
(
    id              SERIAL PRIMARY KEY,
    tidspunkt       TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    saksbehandlerid TEXT                                                        NOT NULL,
    behandlingid    UUID UNIQUE                                                 NOT NULL,
    vedtak          JSONB                                                       NOT NULL,
    request         JSONB                                                       NOT NULL,
    response        JSONB                                                       NOT NULL
)
;

CREATE INDEX ON simulering (behandlingid)
;
