create table beregning_refresh
(
    behandling_id        uuid primary key,
    sak_id               bigint,
    beregning_behandling jsonb  default null,
    hentet_status        text   default 'IKKE_HENTET',
    patchet_status       text   default 'IKKE_PATCHET',
    antall_sak_fix       bigint default 0,
    antall_stoenad_fix   bigint default 0
);

with beregninger as (select behandling_id, sak_id
                     from sak
                     where beregning != 'null'
                     union
                     select behandlingid as behandling_id, sakid as sak_id
                     from stoenad
                     where beregning != 'null')
insert
into beregning_refresh (behandling_id, sak_id)
select behandling_id, sak_id
from beregninger;
