-- Fjerne not null på bytes og legge til felt for payload
alter table innhold
    alter column bytes drop not null;
alter table innhold
    add payload text;

-- Legge til ny prosesstype for å indikere hvordan brevet skal opprettes
alter table brev
    add prosess_type text not null default 'AUTOMATISK';
alter table brev
    alter column prosess_type drop default;

-- behandling_id skal være unik for en behandling, og blir derfor vedtaksbrev
alter table brev
    alter column behandling_id drop not null;
alter table brev
    add unique (behandling_id);
alter table brev
    drop column vedtaksbrev;

-- Fjerne rang fra status siden det ikke brukes
alter table status
    drop column rang;
