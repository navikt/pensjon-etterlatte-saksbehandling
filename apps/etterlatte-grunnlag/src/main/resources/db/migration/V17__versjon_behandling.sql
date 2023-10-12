CREATE TABLE behandling_versjon
(
    behandling_id  UUID UNIQUE   NOT NULL,
    sak_id         BIGINT UNIQUE NOT NULL,
    hendelsenummer BIGINT        NOT NULL,
    laast          BOOLEAN       NOT NULL
);

CREATE INDEX ON behandling_versjon (behandling_id, sak_id);