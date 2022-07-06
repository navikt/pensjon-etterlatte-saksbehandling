UPDATE behandling SET oppgave_status = 'NY';

ALTER TABLE behandling
    ALTER COLUMN oppgave_status SET NOT NULL;

ALTER TABLE behandling
    ADD COLUMN behandlingstype VARCHAR NOT NULL DEFAULT 'FØRSTEGANGSBEHANDLING';

ALTER TABLE behandling
    ALTER COLUMN behandlingstype DROP DEFAULT;
