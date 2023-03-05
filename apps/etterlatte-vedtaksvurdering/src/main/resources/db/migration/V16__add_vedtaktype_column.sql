ALTER TABLE vedtak ADD COLUMN vedtaktype VARCHAR;
UPDATE vedtak SET vedtaktype = 'INNVILGELSE' WHERE behandlingtype in ('FØRSTEGANGSBEHANDLING', 'OMREGNING');
UPDATE vedtak SET vedtaktype = 'OPPHOER' WHERE behandlingtype = 'MANUELT_OPPHOER';