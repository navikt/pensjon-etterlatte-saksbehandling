CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE outbox_vedtakshendelse
(
    id        UUID PRIMARY KEY         DEFAULT (uuid_generate_v4()),
    vedtakId  BIGINT                                                      NOT NULL REFERENCES vedtak (id),
    type      TEXT                                                        NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT (now() AT TIME ZONE 'UTC') NOT NULL,
    publisert BOOLEAN                  DEFAULT FALSE
);

CREATE INDEX ON outbox_vedtakshendelse (publisert);
