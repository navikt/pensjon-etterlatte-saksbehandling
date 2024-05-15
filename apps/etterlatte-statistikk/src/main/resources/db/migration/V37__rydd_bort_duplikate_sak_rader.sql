-- Disse behandlingene har duplikate gjenopprettet-meldinger for opprettelse av behandlinger
with duplikater_fra_gjenoppretting as (select behandling_id from sak where kilde = 'GJENOPPRETTA' and behandling_status='OPPRETTET' group by behandling_id having count(*) = 2)
-- for de duplikate gjenopprettet-meldingene har en av de satt pesysid, mens en har ikke. Vi beholder den som har satt pesysid
delete from sak where id in (select id from sak where pesysid is null and sak.behandling_status = 'OPPRETTET' and sak.behandling_id in (select * from duplikater_fra_gjenoppretting));
