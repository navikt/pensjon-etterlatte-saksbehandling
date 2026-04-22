UPDATE vedtak
SET vedtakstatus         = 'RETURNERT',
    attestant            = NULL,
    datoattestert        = NULL,
    attestertvedtakenhet = NULL,
WHERE behandlingid = '75d3a456-5728-445c-a836-e2c7be37010d'
  AND vedtakstatus = 'SAMORDNET';
