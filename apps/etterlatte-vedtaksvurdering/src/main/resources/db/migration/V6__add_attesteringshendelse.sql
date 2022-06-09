CREATE TABLE attesteringshendelse
(
    id BIGSERIAL PRIMARY KEY,
    hendelse TEXT NOT NULL,
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vedtakid BIGINT NOT NULL,
    ident TEXT NOT NULL,
    kommentar TEXT,
    valgtbegrunnelse TEXT
);