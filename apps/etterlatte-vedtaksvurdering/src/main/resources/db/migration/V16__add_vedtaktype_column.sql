ALTER TABLE vedtak ADD COLUMN type VARCHAR;
UPDATE vedtak SET type = 'INNVILGELSE' WHERE behandlingtype in ('FØRSTEGANGSBEHANDLING', 'OMREGNING');
UPDATE vedtak SET type = 'OPPHOER' WHERE behandlingtype = 'MANUELT_OPPHOER';