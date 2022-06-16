-- saksnummer kan ikke være unikt i databasen - det kan komme
--  tilbakekreving på tilbakekreving. Primærnøkkel
-- basert på vedtakid fra TBK i kombinasjon med Saksid?

CREATE TABLE kravgrunnlag
(
    kravgrunnlag_id        BIGINT PRIMARY KEY,
    sak_id                 BIGINT      NOT NULL,
    vedtak_id              BIGINT      NOT NULL,
    behandling_id          UUID DEFAULT NULL,
    kontrollfelt           VARCHAR     NOT NULL,
    status                 VARCHAR(32) NOT NULL, -- enum?
    saksbehandler          VARCHAR(32) NOT NULL,
    siste_utbegalingslinje VARCHAR     NOT NULL  -- siste gjeldende utbetalingslinjeid?
);

CREATE TABLE grunnlagsperiode
(
    id               UUID PRIMARY KEY,
    fra_og_med       DATE                                             NOT NULL,
    til_og_med       DATE                                             NOT NULL,
    kravgrunnlag_id  BIGINT references kravgrunnlag (kravgrunnlag_id) NOT NULL,
    beloep_skatt_mnd DECIMAL
);

CREATE TABLE
(
    id                              UUID PRIMARY KEY,
    kode                            VARCHAR NOT NULL,
    type                            VARCHAR NOT NULL, -- enum?
    beloep_til_tidligere_utbetaling DECIMAL NOT NULL,
    beloep_ny_utbetaling            DECIMAL NOT NULL,
    beloep_skal_tilbakekreves       DECIMAL NOT NULL,
    beloep_skal_ikke_tilbakekreved  DECIMAL NOT NULL,
    skatteprosent                   DECIMAL NOT NULL
)


-- enda en tabell for vedtaket som fattes av saksbehandler?