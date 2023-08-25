begin transaction;
with ids as (
    select distinct b.id
    from brev b
             inner join hendelse h on b.id = h.brev_id
    inner join innhold i on b.id = i.brev_id
    where b.id not in (select distinct brev_id
                       from hendelse
                       where status_id in ('FERDIGSTILT', 'JOURNALFOERT', 'DISTRIBUERT'))
      and b.prosess_type = 'AUTOMATISK'
    and i.tittel = 'Vedtak om innvilgelse'
)
update innhold set payload='[]' where brev_id in (select id from ids);

with ids as (
    select distinct b.id
    from brev b
             inner join hendelse h on b.id = h.brev_id
             inner join innhold i on b.id = i.brev_id
    where b.id not in (select distinct brev_id
                       from hendelse
                       where status_id in ('FERDIGSTILT', 'JOURNALFOERT', 'DISTRIBUERT'))
      and b.prosess_type = 'AUTOMATISK'
      and i.tittel = 'Vedtak om innvilgelse'
)


update brev set prosess_type = 'REDIGERBAR' where id in (select id from ids);


commit transaction;