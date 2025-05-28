ALTER TABLE etteroppgjoer_behandling ALTER COLUMN har_mottatt_ny_informasjon TYPE TEXT;
ALTER TABLE etteroppgjoer_behandling ALTER COLUMN endring_er_til_ugunst_for_bruker TYPE TEXT;

UPDATE etteroppgjoer_behandling SET har_mottatt_ny_informasjon = 'JA' WHERE har_mottatt_ny_informasjon = 'true';
UPDATE etteroppgjoer_behandling SET har_mottatt_ny_informasjon = 'NEI' WHERE har_mottatt_ny_informasjon = 'false';

UPDATE etteroppgjoer_behandling SET endring_er_til_ugunst_for_bruker = 'JA' WHERE endring_er_til_ugunst_for_bruker = 'true';
UPDATE etteroppgjoer_behandling SET endring_er_til_ugunst_for_bruker = 'NEI' WHERE endring_er_til_ugunst_for_bruker = 'false';