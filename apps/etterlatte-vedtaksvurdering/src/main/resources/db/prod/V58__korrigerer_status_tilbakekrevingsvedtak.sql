UPDATE vedtak
SET vedtakstatus      = 'OPPRETTET',
    saksbehandlerid   = null,
    datofattet        = null,
    fattetvedtakenhet = null
WHERE id = 64597
  AND vedtakstatus = 'FATTET_VEDTAK'
  AND sakid = 13090;

UPDATE vedtak
SET vedtakstatus      = 'OPPRETTET',
    saksbehandlerid   = null,
    datofattet        = null,
    fattetvedtakenhet = null
WHERE id = 64272
  AND vedtakstatus = 'FATTET_VEDTAK'
  AND sakid = 20636;

UPDATE vedtak
SET vedtakstatus         = 'FATTET_VEDTAK',
    attestertvedtakenhet = null,
    attestant            = null,
    datoattestert        = null
WHERE id = 59970
  AND vedtakstatus = 'ATTESTERT'
  AND sakid = 21803;