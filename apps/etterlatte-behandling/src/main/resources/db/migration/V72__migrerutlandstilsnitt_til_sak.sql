ALTER TABLE sak ADD COLUMN utenlandstilsnitt JSONB;

update sak
set utenlandstilsnitt = behandlingUtlandtilsnitt.utenlandstilsnitt::jsonb
from (select utenlandstilsnitt, sak_id, id from behandling b1 where utenlandstilsnitt is not null AND sist_endret = (select max(sist_endret) from behandling b2 where b2.sak_id = b1.sak_id)) as behandlingUtlandtilsnitt
where behandlingUtlandtilsnitt.sak_id = sak.id