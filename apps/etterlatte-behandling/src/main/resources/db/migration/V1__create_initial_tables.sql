CREATE TABLE sak
(
    id BIGSERIAL
            PRIMARY KEY,
    fnr VARCHAR,
    sakType VARCHAR
);


CREATE TABLE behandling
(
    id UUID
            PRIMARY KEY,
    sak_id BIGINT NOT NULL
        CONSTRAINT behandling_sak_id_fk
            REFERENCES sak (id),
    vilkaarsproving VARCHAR,
    beregning VARCHAR,
    fastsatt boolean
);

CREATE TABLE opplysning
(
    id UUID
        PRIMARY KEY,
    data VARCHAR,
    meta VARCHAR,
    kilde VARCHAR,
    type VARCHAR
);

CREATE TABLE opplysning_i_behandling
(
    behandling_id UUID NOT NULL
        CONSTRAINT opplysning_i_behandling_fk1
            REFERENCES behandling (id),
    opplysning_id UUID NOT NULL
        CONSTRAINT opplysning_i_behandling_fk2
            REFERENCES opplysning (id)
);

CREATE TABLE opplysning_i_opplysning
(
    avledet_opplysning UUID NOT NULL
        CONSTRAINT opplysning_i_opplysning_fk1
            REFERENCES opplysning (id),
    opplysning UUID NOT NULL
        CONSTRAINT opplysning_i_opplysning_fk2
            REFERENCES opplysning (id)
);