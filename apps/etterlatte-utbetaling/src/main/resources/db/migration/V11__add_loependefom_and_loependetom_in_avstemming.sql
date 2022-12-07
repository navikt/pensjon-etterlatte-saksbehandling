ALTER TABLE avstemming
    ALTER COLUMN periode_fra DROP NOT NULL,
    ALTER COLUMN periode_til DROP NOT NULL,
    ALTER COLUMN antall_oppdrag DROP NOT NULL,
    ADD COLUMN loepende_fom          TIMESTAMP WITH TIME ZONE,
    ADD COLUMN opprettet_tom         TIMESTAMP WITH TIME ZONE,
    ADD COLUMN loepende_utbetalinger TEXT;
