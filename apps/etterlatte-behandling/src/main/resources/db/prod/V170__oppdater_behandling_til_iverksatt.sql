-- Oppdaterer status til IVERKSATT for behandling som har iverksatt vedtak og godkjent utbetaling men har feilet ved oppdatering av status
UPDATE behandling SET status = 'IVERKSATT' WHERE id = 'e6de3a20-b586-4fad-8335-3b50b8067b9e';

-- Legger til hendelse for at dataene skal være konsistent
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 27557, 'e6de3a20-b586-4fad-8335-3b50b8067b9e'::UUID, 9785, NULL, NULL, NULL, NULL);
