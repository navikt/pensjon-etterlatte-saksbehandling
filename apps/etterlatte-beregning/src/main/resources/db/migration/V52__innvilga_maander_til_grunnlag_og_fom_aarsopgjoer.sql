-- Antall innvilga måneder kan ikke være på årsoppgjør nivå da det må tas høyde for uforutsigbare oppphør underveis i året
ALTER TABLE avkorting_aarsoppgjoer
    ALTER COLUMN innvilga_maaneder DROP NOT NULL;

-- For å enkelt vite når første fom for alle perioder er
ALTER TABLE avkorting_aarsoppgjoer
    ADD COLUMN fom DATE;

-- Finner første fom ved å finne antall IKKE innvilga måneder og plusse fra januer
UPDATE avkorting_aarsoppgjoer
SET fom = (date('2024-01-01') + ((12 - subquery.innvilga_maaneder::int8)::text || ' month')::interval)::date
FROM (SELECT innvilga_maaneder FROM avkorting_aarsoppgjoer) subquery;

ALTER TABLE avkorting_aarsoppgjoer
    ALTER COLUMN fom SET NOT NULL;

-- Flytter innvilga måneder fra årsoppgjør til grunnlag
-- Dette kjøres i første år til OMS så det er ingen perioder hvor antall innvilga varierer enda
UPDATE avkortingsgrunnlag
SET relevante_maaneder = aarsoppgjoer.innvilga_maaneder
FROM (SELECT * FROM avkorting_aarsoppgjoer) aarsoppgjoer
WHERE avkortingsgrunnlag.aarsoppgjoer_id = aarsoppgjoer.id;