CREATE TABLE oppgave_kommentar (
    id UUID PRIMARY KEY,
    sak_id BIGINT NOT NULL
       CONSTRAINT behandling_sak_id_fk
           REFERENCES sak (id),
    oppgave_id UUID,
    kommentar TEXT,
    tidspunkt TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    saksbehandler TEXT
)