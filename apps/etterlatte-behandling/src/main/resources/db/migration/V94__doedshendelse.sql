CREATE TABLE doedshendelse
(
    id TEXT PRIMARY KEY,
    avdoed_fnr TEXT NOT NULL,
    avdoed_doedsdato DATE NOT NULL,
    beroert_fnr TEXT NOT NULL,
    relasjon TEXT NOT NULL,
    opprettet TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status TEXT NOT NULL,
    utfall TEXT,
    oppgave_id TEXT,
    brev_id BIGINT
)