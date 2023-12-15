update sak s
set teknisk_tid = s.teknisk_tid + interval '1 hour'
where s.behandling_status = 'IVERKSATT'
  and s.saksbehandler = 'EY'
  and s.teknisk_tid < (select s2.teknisk_tid
                       from sak s2
                       where s2.behandling_status = 'ATTESTERT' and s.behandling_id = s2.behandling_id);