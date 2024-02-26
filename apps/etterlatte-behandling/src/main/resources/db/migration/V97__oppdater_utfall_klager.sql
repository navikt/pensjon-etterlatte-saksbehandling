update klage
set utfall = jsonb_set(utfall, '{innstilling, innstillingTekst}', '"Innstilling"')
where utfall -> 'innstilling' is not null;
