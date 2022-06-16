ALTER TABLE utbetaling ALTER COLUMN behandling_id_til_oppdrag SET NOT NULL;

ALTER TABLE utbetalingslinje DROP CONSTRAINT utbetalingslinje_utbetaling_id_fkey;
ALTER TABLE utbetalingslinje ALTER COLUMN utbetaling_id TYPE uuid USING utbetaling_id::uuid;
ALTER TABLE utbetaling ALTER COLUMN id TYPE uuid USING id::uuid;
ALTER TABLE utbetaling ALTER COLUMN behandling_id TYPE uuid USING id::uuid;
ALTER TABLE utbetalingslinje ADD CONSTRAINT utbetalingslinje_utbetaling_id_fkey FOREIGN KEY (utbetaling_id) REFERENCES utbetaling (id);
