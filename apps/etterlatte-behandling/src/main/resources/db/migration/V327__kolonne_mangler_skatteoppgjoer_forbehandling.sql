ALTER TABLE etteroppgjoer_behandling ADD COLUMN mottatt_skatteoppgjoer BOOLEAN;

/* setter mottatt skatteoppgjoer til true for alle opprettede forbehandlinger */
UPDATE etteroppgjoer_behandling SET mottatt_skatteoppgjoer = true;
