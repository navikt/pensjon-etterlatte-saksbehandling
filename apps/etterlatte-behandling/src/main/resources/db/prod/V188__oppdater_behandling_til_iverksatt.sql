-- Oppdaterer status til IVERKSATT for behandling som har iverksatt vedtak og godkjent utbetaling men har feilet ved oppdatering av status
UPDATE behandling SET status = 'IVERKSATT' WHERE id = '8bb69404-ce9e-4621-8381-789e484aa208';

-- Legger til hendelse for at dataene skal være konsistent
INSERT INTO behandlinghendelse(hendelse, inntruffet, vedtakid, behandlingid, sakid, ident, identtype, kommentar, valgtbegrunnelse)
VALUES ('VEDTAK:IVERKSATT', now()::timestamp, 35771, '8bb69404-ce9e-4621-8381-789e484aa208'::UUID, 10125, NULL, NULL, NULL, NULL);
