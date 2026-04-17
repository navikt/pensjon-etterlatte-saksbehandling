-- Legg til behandlingId for vedtak som skal sendes på nytt til utbetaling.
-- Opprett en ny migrasjon per incident og fyll inn korrekt UUID.
INSERT INTO resend_til_utbetaling (behandling_id) VALUES ('75d3a456-5728-445c-a836-e2c7be37010d');