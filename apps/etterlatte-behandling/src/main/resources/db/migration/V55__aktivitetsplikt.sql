CREATE TABLE aktivitetsplikt_oppfolging
(
    id            SERIAL,
    behandling_id UUID references behandling (id),
    aktivitet     TEXT,
    opprettet_av  TEXT,
    opprettet     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ON aktivitetsplikt_oppfolging (behandling_id);
