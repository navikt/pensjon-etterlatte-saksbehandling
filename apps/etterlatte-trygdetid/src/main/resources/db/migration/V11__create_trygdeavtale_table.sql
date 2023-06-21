CREATE TABLE trygdeavtale
(
    id                   UUID PRIMARY KEY,
    behandling_id        UUID NOT NULL,
    avtale_kode          TEXT NOT NULL,
    avtale_dato_kode     TEXT,
    avtale_kriteria_kode TEXT,
    kilde                TEXT
);
