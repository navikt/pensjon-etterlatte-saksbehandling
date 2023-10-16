create table sjekkliste
(
    id           UUID PRIMARY KEY,
    kommentar    TEXT,
    adresse_brev TEXT,
    kontonr_reg  TEXT,
    bekreftet    BOOLEAN   NOT NULL DEFAULT FALSE,
    opprettet_av TEXT      NOT NULL,
    opprettet    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret_av    TEXT      NOT NULL,
    endret       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versjon      BIGINT    NOT NULL DEFAULT 1
);

create table sjekkliste_item
(
    id           SERIAL PRIMARY KEY,
    sjekkliste   UUID      NOT NULL,
    beskrivelse  TEXT      NOT NULL,
    avkrysset    BOOLEAN   NOT NULL DEFAULT FALSE,
    opprettet_av TEXT      NOT NULL,
    opprettet    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret_av    TEXT      NOT NULL,
    endret       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versjon      BIGINT    NOT NULL DEFAULT 1,
    constraint fk_sjekkliste
        foreign key (sjekkliste)
            references sjekkliste (id)
);
