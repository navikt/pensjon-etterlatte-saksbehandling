create temporary view tidligste_attestering_per_behandling as
(
select distinct ON (behandling_id) behandling_id, id
from sak
where behandling_status = 'ATTESTERT'
order by behandling_id, id ASC
    );

delete from sak where id in (
    select s.id
    from sak s
             inner join tidligste_attestering_per_behandling tapb on s.behandling_id = tapb.behandling_id
    where s.behandling_status = 'ATTESTERT'
      and s.id != tapb.id -- Er ikke den tidligste attesteringen på denne behandlingen
);
