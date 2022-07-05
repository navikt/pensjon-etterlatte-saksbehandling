CREATE TABLE behandlinghendelse
(
    id BIGSERIAL PRIMARY KEY,
    hendelse TEXT NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    inntruffet TIMESTAMP WITH TIME ZONE,
    vedtakid BIGINT,
    behandlingid UUID NOT NULL,
    sakid BIGINT NOT NULL,
    ident TEXT,
    identtype TEXT,
    kommentar TEXT,
    valgtbegrunnelse TEXT
);
