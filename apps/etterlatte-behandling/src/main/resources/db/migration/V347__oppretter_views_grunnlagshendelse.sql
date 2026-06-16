-- Indeks for å støtte DISTINCT ON-spørringene i viewsene under.
-- Dekker (sak_id, opplysning_type, fnr) med synkende hendelsenummer slik at Postgres
-- kan lese én rad per gruppe uten full tabellskanning.
CREATE INDEX grunnlagshendelse_siste_per_type_idx
    ON grunnlagshendelse (sak_id, opplysning_type, fnr, hendelsenummer DESC);

-- Siste opplysning per (sak_id, opplysning_type, fnr).
-- Brukes for oppslag direkte på sak uten versjonsbegrensning.
CREATE VIEW siste_grunnlagshendelse AS
SELECT DISTINCT ON (sak_id, opplysning_type, fnr)
    sak_id,
    opplysning_id,
    kilde,
    opplysning_type,
    opplysning,
    hendelsenummer,
    fnr,
    fom,
    tom
FROM grunnlagshendelse
ORDER BY sak_id, opplysning_type, fnr, hendelsenummer DESC;

-- Siste opplysning per (behandling_id, opplysning_type, fnr), begrenset til
-- hendelsenummeret som behandlingen er låst til i behandling_versjon.
-- Erstatter LEFT JOIN + korrelert subspørring/ORDER BY-mønsteret i OpplysningDao.
CREATE VIEW siste_grunnlagshendelse_per_behandling AS
SELECT DISTINCT ON (bv.behandling_id, gh.opplysning_type, gh.fnr)
    bv.behandling_id,
    bv.sak_id,
    gh.opplysning_id,
    gh.kilde,
    gh.opplysning_type,
    gh.opplysning,
    gh.hendelsenummer,
    gh.fnr,
    gh.fom,
    gh.tom
FROM grunnlagshendelse gh
         JOIN behandling_versjon bv
              ON bv.sak_id = gh.sak_id AND gh.hendelsenummer <= bv.hendelsenummer
ORDER BY bv.behandling_id, gh.opplysning_type, gh.fnr, gh.hendelsenummer DESC;