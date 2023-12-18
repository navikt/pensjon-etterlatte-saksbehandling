create temporary view saker_med_offset_teknisk_tid as
(
select b_iverksatt.behandling_id, b_iverksatt.teknisk_tid
from sak b_iverksatt
         left join sak b_attestert on b_iverksatt.behandling_id = b_attestert.behandling_id
where b_iverksatt.behandling_status = 'IVERKSATT'
  and b_attestert.behandling_status = 'ATTESTERT'
  and b_iverksatt.teknisk_tid < b_attestert.teknisk_tid
    );

update sak s
set teknisk_tid = s.teknisk_tid + interval '1 hour'
from saker_med_offset_teknisk_tid saker_med_feil
where s.behandling_id = saker_med_feil.behandling_id
  and s.saksbehandler = 'EY'
  and s.behandling_status = 'IVERKSATT';
