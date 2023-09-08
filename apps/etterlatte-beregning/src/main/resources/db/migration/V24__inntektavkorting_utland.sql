ALTER TABLE avkortingsgrunnlag ADD COLUMN inntekt_utland BIGINT;
UPDATE avkortingsgrunnlag set inntekt_utland = 0;

ALTER TABLE avkortingsgrunnlag ADD COLUMN fratrekk_inn_aar_utland BIGINT;
UPDATE avkortingsgrunnlag set fratrekk_inn_aar_utland = 0;
