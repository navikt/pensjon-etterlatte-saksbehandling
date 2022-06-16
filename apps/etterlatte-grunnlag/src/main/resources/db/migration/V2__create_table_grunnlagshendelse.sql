CREATE TABLE grunnlagshendelse
(
    sak_id         BIGINT NOT NULL,
    opplysning_id UUID,
    opplysning     TEXT,
    kilde          TEXT,
    opplysning_type TEXT,
    hendelsenummer BIGINT NOT NULL,
    PRIMARY KEY (sak_id, hendelsenummer)
);