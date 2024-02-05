CREATE TABLE vent
(
    id           uuid PRIMARY KEY,
    oppgaveId    uuid NULL
        CONSTRAINT vent_oppgave_fk_id
            REFERENCES oppgave (id),
    behandlingID uuid NULL
        CONSTRAINT vent_behandling_fk_id
            REFERENCES behandling (id),
    opprettet timestamp NOT NULL default current_timestamp,
    haandtert boolean not null default false,
    paa_vent_til DATE not null,
    ventetype varchar not null
)