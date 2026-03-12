UPDATE etteroppgjoer_behandling
SET mottatt_skatteoppgjoer  = true,
WHERE sak_id = 12163
    AND mottatt_skatteoppgjoer = false;
