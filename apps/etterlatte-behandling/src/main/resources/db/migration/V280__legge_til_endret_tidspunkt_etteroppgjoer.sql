ALTER TABLE etteroppgjoer ADD COLUMN endret TIMESTAMP;
UPDATE etteroppgjoer SET endret = opprettet WHERE endret IS NULL;