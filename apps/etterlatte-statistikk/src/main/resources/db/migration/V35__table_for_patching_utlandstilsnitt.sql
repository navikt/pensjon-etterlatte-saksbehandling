create table utlandstilknytning_fiksing
(
    behandling_id                 uuid primary key,
    sak_id                        bigint,
    utlandstilknytning_behandling text   default null,
    hentet_status                 text   default 'IKKE_HENTET',
    patch_status                  text   default 'IKKE_PACHET',
    antall_sak_fix                bigint default 0,
    antall_stoenad_fix            bigint default 0
);

with behandlinger as
         (select distinct behandling_id, sak_id
          from sak
          union
          select distinct behandlingid as behandling_id, sakid as sak_id
          from stoenad)
insert
into utlandstilknytning_fiksing (behandling_id, sak_id)
select distinct behandling_id, sak_id
from behandlinger;
