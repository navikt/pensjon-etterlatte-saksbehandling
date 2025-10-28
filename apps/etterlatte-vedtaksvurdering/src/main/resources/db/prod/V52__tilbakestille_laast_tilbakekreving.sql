-- Setter riktig status i vedtak om tilbakekreving
UPDATE vedtak
SET vedtakstatus         = 'FATTET_VEDTAK',
    attestertvedtakenhet = null,
    attestant            = null,
    datoattestert        = null
WHERE id = 59970
  and vedtakstatus = 'ATTESTERT'
  and type = 'TILBAKEKREVING'
  and sakid = 21803;
