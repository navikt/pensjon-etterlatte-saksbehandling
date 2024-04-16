CREATE TABLE sanksjon(
    id UUID PRIMARY KEY,
    behandling_id UUID NOT NULL,
    sak_id BIGINT NOT NULL,
    fom Date,
    tom Date,
    saksbehandler TEXT,
    opprettet TIMESTAMP,
    endret TIMESTAMP,
    beskrivelse TEXT
);