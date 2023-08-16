CREATE TABLE oppgaveendringer (
    id UUID PRIMARY KEY,
    oppgaveId UUID NOT NULL,
    oppgaveFoer JSONB,
    oppgaveEtter JSONB,
    tidspunkt TIMESTAMP
);