create table behandling_mangler_avbrudd_statistikk
(
    behandling_id    UUID primary key,
    sak_id           BIGINT,
    mangler_hendelse BOOL default false,
    sendt_melding    BOOL default false
);

-- Populerer tabellen med behandlinger som ikke har fått en behandlinghendelse med avbrutt,
-- siden de har blitt avbrutt utenfor standard flyt og dermed har de heller ikke sendt melding
-- til statistikk
insert into behandling_mangler_avbrudd_statistikk (behandling_id, sak_id, mangler_hendelse)
select b.id, sak_id, true
from behandling b
where b.status = 'AVBRUTT'
  and not exists(select 1 from behandlinghendelse where behandlingid = b.id and hendelse = 'BEHANDLING:AVBRUTT');
