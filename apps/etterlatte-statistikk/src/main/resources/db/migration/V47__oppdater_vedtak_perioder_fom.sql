create table oppdaterte_vedtak_opphoer_fom_perioder
(
    behandling_id   uuid primary key,
    vedtak_id       int,
    hentet_status   text  default 'IKKE_HENTET',
    patchet_status  text  default 'IKKE_PATCHET',
    opphoer_fom     date  default null,
    vedtaksperioder jsonb default null
);

with kjente_vedtak as (select distinct behandlingid as behandling_id from stoenad)
insert
into oppdaterte_vedtak_opphoer_fom_perioder (behandling_id, vedtak_id)
select (behandling_id, null)
from kjente_vedtak;
