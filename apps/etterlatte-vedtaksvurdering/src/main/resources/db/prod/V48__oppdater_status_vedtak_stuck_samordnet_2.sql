-- bare manuelt setter iverksatt til å ha skjedd kort etter attestering
update vedtak
set datoiverksatt = (datoattestert::timestamp + interval '1 minute')::timestamp,
    vedtakstatus  = 'IVERKSATT'
where sakid in (23574, 22642, 24006)
  and id in (60104, 59374, 60208)
  and vedtakstatus = 'SAMORDNET';