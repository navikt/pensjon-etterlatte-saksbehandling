-- Setter riktig status i vedtak om tilbakekreving som er låst
UPDATE vedtak
SET vedtakstatus         = 'FATTET_VEDTAK',
    attestertvedtakenhet = null,
    attestant            = null,
    datoattestert        = null
WHERE id in (60162, 60204, 60235)
  and vedtakstatus = 'ATTESTERT'
  and type = 'TILBAKEKREVING';
