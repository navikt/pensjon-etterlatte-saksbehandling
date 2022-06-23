CREATE TABLE tilbakekreving
(
    kravgrunnlag_id BIGINT PRIMARY KEY,
    sak_id          BIGINT                   NOT NULL,
    behandling_id   UUID                     NOT NULL,
    opprettet       TIMESTAMP WITH TIME ZONE NOT NULL,
    kravgrunnlag    JSONB                    NOT NULL,
    vedtak          JSONB,
    attestasjon     JSONB
);