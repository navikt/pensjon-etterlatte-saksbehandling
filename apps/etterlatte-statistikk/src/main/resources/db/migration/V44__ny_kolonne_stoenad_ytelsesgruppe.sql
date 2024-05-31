alter table stoenad add column sakYtelsesgruppe text;
alter table maaned_stoenad add column sakYtelsesgruppe text;

update stoenad
set sakytelsesgruppe = sak_spoerring.sak_ytelsesgruppe
from (select distinct behandling_id, sak_ytelsesgruppe from sak where sak_ytelsesgruppe is not null) as sak_spoerring
where behandlingid = sak_spoerring.behandling_id;

update maaned_stoenad
set sakytelsesgruppe = sak_spoerring.sak_ytelsesgruppe
from (select distinct behandling_id, sak_ytelsesgruppe from sak where sak_ytelsesgruppe is not null) as sak_spoerring
where behandlingid = sak_spoerring.behandling_id;

-- Refresh av view for forrige maanedes statistikk for import, for å få med ny kolonne
CREATE OR REPLACE VIEW maaned_stoenad_statistikk AS
SELECT *
FROM maaned_stoenad
WHERE extract(MONTH FROM registrertTimestamp) = extract(MONTH FROM NOW());