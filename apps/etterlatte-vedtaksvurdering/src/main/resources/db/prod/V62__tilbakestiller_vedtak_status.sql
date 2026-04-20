-- Setter riktig status i automatisk vedtak om opphør aldersovergang
UPDATE vedtak
SET vedtakstatus      = 'OPPRETTET',
    fattetvedtakenhet = null,
    datofattet        = null,
    saksbehandlerid   = null
WHERE id = 69169
  and behandlingid = '8a780fe4-8d62-487d-9e5a-0ee1e2ac2761';
