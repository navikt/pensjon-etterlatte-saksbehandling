-- Setter riktig status i vedtak om tilbakekreving, som gikk i vranglås igjen
UPDATE vedtak
SET vedtakstatus         = 'FATTET_VEDTAK',
    attestertvedtakenhet = null,
    attestant            = null,
    datoattestert        = null
WHERE id = 59970
  and sakid = 21803;
