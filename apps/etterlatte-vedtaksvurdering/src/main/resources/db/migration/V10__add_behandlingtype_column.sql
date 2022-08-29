ALTER TABLE vedtak
    ADD COLUMN behandlingtype TEXT;
UPDATE vedtak SET behandlingtype = 'FØRSTEGANGSBEHANDLING';