-- Tilbakekrevingen feilet mot tilbakekrevingskomponenten
-- og havnet nok en gang i låst tilstand
UPDATE vedtak
SET vedtakstatus         = 'FATTET_VEDTAK',
    attestertvedtakenhet = null,
    attestant            = null,
    datoattestert        = null
WHERE id = 63038
  and sakid = 17565;
