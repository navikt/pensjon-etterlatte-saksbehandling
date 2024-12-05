CREATE TABLE aktivitetsplikt_hendelse
(
    id            UUID UNIQUE,
    sak_id        BIGINT NOT NULL,
    behandling_id UUID,
    dato          DATE,
    opprettet     TEXT,
    endret        TEXT,
    beskrivelse   TEXT,
    CONSTRAINT fk_sak_id FOREIGN KEY (sak_id) REFERENCES sak (id)
);