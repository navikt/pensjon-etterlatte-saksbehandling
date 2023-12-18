create temporary view saker_med_offset_teknisk_tid as
(
select s_iverksatt.behandling_id, s_iverksatt.teknisk_tid
from sak s_iverksatt
         left join sak b_attestert on s_iverksatt.behandling_id = b_attestert.behandling_id
where s_iverksatt.behandling_status = 'IVERKSATT'
  and b_attestert.behandling_status = 'ATTESTERT'
  and s_iverksatt.teknisk_tid < b_attestert.teknisk_tid
    );

update sak s
set teknisk_tid = s.teknisk_tid + interval '1 hour'
from saker_med_offset_teknisk_tid saker_med_feil
where s.behandling_id = saker_med_feil.behandling_id
  and s.behandling_status = 'IVERKSATT';
