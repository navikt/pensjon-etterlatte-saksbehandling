update sak s
set ansvarlig_enhet = (select distinct ansvarlig_enhet
                       from sak s2
                       where s2.ansvarlig_enhet is not null
                         and s2.behandling_id = s.behandling_id)
where s.ansvarlig_enhet is null
  and s.behandling_status in ('FATTET', 'UNDERKJENT')
  and (select count(distinct ansvarlig_enhet)
       from sak s2
       where s2.ansvarlig_enhet is not null
         and s2.behandling_id = s.behandling_id) = 1;