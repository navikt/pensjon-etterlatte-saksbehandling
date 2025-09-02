-- bare manuelt setter iverksatt til å ha skjedd kort etter attestering
update vedtak
set datoiverksatt = (datoattestert::timestamp + interval '1 minute')::timestamp,
    vedtakstatus  = 'IVERKSATT'
where sakid = 20602
  and id = 60094
  and vedtakstatus = 'SAMORDNET';