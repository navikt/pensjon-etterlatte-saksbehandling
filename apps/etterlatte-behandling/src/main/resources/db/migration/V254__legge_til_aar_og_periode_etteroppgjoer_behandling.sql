ALTER TABLE etteroppgjoer_behandling
    ADD COLUMN aar INT;
update etteroppgjoer_behandling
set aar = 2024;
ALTER TABLE etteroppgjoer_behandling
    ALTER COLUMN aar SET NOT NULL;

ALTER TABLE etteroppgjoer_behandling
    ADD COLUMN fom DATE;
-- kun test behandlinger som finnes så kan sette en dato som ikke stemmer
update etteroppgjoer_behandling
set fom = '2024-01-01'
where fom is null;
ALTER TABLE etteroppgjoer_behandling
    ALTER COLUMN fom SET NOT NULL;

ALTER TABLE etteroppgjoer_behandling
    ADD COLUMN tom DATE;
update etteroppgjoer_behandling
set tom = '2024-12-31'
where tom is null;
ALTER TABLE etteroppgjoer_behandling
    ALTER COLUMN tom SET NOT NULL;
