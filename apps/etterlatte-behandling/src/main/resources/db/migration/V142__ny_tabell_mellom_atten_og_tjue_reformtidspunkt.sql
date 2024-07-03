
CREATE TABLE mellom_atten_og_tjue_ved_reformtidspunkt
(
    id BIGSERIAL
        PRIMARY KEY,
    fnr TEXT NOT NULL,
    opprettet TIMESTAMP NOT NULL,
    endret TIMESTAMP NOT NULL,
    status TEXT NOT NULL,
    feilmelding TEXT
);