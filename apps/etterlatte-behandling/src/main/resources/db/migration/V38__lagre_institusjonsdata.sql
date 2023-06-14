CREATE TABLE institusjonsoppholdhendelse
(
    id            UUID PRIMARY KEY,
    sak_id        BIGINT NOT NULL,
    kanGiReduksjon          TEXT,
    kanGiReduksjonTekst     TEXT,
    merEnnTreMaaneder          TEXT,
    merEnnTreMaanederTekst     TEXT,
    saksbehandler               TEXT
)