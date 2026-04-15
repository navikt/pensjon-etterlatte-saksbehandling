-- Legg til behandlingId for vedtak som skal sendes på nytt til utbetaling.
-- Opprett en ny migrasjon per incident og fyll inn korrekt UUID.
-- Slett utbetalingen som har feilet, slik at den kan opprettes på nytt.
INSERT INTO resend_til_utbetaling (behandling_id) VALUES ('75d3a456-5728-445c-a836-e2c7be37010d');
DELETE FROM utbetaling WHERE id = 'ff5cb526-cbb8-43b2-abbf-9c4538fb7307' AND vedtak_id = '68905';