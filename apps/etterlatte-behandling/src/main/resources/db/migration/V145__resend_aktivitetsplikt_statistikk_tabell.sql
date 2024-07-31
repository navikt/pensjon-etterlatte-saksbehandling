create table resend_aktivitetsplikt_statistikk
(
    sak_id BIGINT PRIMARY KEY,
    status TEXT
);

with saker_med_aktivitet as (select distinct sak_id, behandling_id
                             from aktivitetsplikt_aktivitetsgrad
                             union
                             select distinct sak_id, behandling_id
                             from aktivitetsplikt_unntak)
insert
into resend_aktivitetsplikt_statistikk (sak_id, status)
select sak_id, 'IKKE_SENDT'
from saker_med_aktivitet
on conflict do nothing;
