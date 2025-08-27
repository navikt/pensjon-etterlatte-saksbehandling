ALTER TABLE etteroppgjoer DROP COLUMN har_bosattutland;

ALTER TABLE etteroppgjoer ADD COLUMN har_adressebeskyttelse BOOLEAN;
ALTER TABLE etteroppgjoer ADD COLUMN har_aktivitetskrav BOOLEAN;
ALTER TABLE etteroppgjoer ADD COLUMN er_bosatt_utland BOOLEAN;