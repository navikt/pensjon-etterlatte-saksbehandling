create table sak_rader_med_potensielt_feil_resultat
(
    id              bigint primary key,
    behandling_id   uuid,
    behandling_type text,
    fikset          boolean default false
);

-- Legger inn saker som kan ha fått feil mapping til innvilgelse
insert into sak_rader_med_potensielt_feil_resultat
select id, behandling_id, behandling_type
from sak
where behandling_type in ('FØRSTEGANGSBEHANDLING', 'REVURDERING')
  and behandling_resultat = 'INNVILGELSE';
