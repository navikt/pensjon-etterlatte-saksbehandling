UPDATE vedtak
SET vedtakstatus  = 'IVERKSATT',
    datoiverksatt = now()
WHERE id = 64987
  AND vedtakstatus = 'SAMORDNET';
