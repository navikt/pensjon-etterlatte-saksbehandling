-- Setter riktig status i vedtak om tilbakekreving
UPDATE vedtak
SET vedtakstatus         = 'FATTET_VEDTAK',
    attestertvedtakenhet = null,
    attestant            = null,
    datoattestert        = null
WHERE id = 60204
  and sakid = 18981;

UPDATE vedtak
SET vedtakstatus         = 'FATTET_VEDTAK',
    attestertvedtakenhet = null,
    attestant            = null,
    datoattestert        = null
WHERE id = 63038
  and sakid = 17565;
