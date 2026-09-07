-- Fiks like teknisk_tid på FATTET- og ATTESTERT-raden for samme behandling.
-- Rotårsak: ved automatisk behandling ble vedtak.datofattet og vedtak.datoattestert
-- satt med Postgres now() i samme databasetransaksjon, som er fryst til transaksjonens
-- starttidspunkt. Dette ga identisk tidspunkt, som ble videreført uendret som teknisk_tid
-- på hhv. FATTET- og ATTESTERT-hendelsen inn i saksstatistikken. Bug fra 2026-05-20,
-- rettet i kildekoden (vedtak-tabellen) i etterlatte-behandling.
-- Her flytter vi kun ATTESTERT-radens teknisk_tid ett millisekund fram, slik at de to
-- radene for samme behandling ikke lenger er identiske.
UPDATE sak s
SET teknisk_tid = teknisk_tid + interval '1 millisecond', tidspunkt_registrert = now()
WHERE behandling_status = 'ATTESTERT'
  AND teknisk_tid >= '2026-05-20'
  AND EXISTS (
      SELECT 1
      FROM sak s2
      WHERE s2.behandling_id = s.behandling_id
        AND s2.behandling_status = 'FATTET'
        AND s2.teknisk_tid = s.teknisk_tid
  );