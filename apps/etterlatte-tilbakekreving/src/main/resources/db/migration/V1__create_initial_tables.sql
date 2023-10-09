CREATE TABLE tilbakekreving_hendelse
(
    id              UUID PRIMARY KEY,
    opprettet       TIMESTAMP WITH TIME ZONE NOT NULL,
    payload         TEXT                     NOT NULL,
    type            TEXT                     NOT NULL,
    kravgrunnlag_id TEXT                     NOT NULL
);