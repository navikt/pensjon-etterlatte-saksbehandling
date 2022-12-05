ALTER TABLE utbetalingslinje
    ADD COLUMN klassifikasjonskode TEXT;


UPDATE utbetalingslinje
SET klassifikasjonskode = 'BARNEPENSJON-OPTP';

ALTER TABLE utbetalingslinje
    ALTER COLUMN klassifikasjonskode SET NOT NULL;

