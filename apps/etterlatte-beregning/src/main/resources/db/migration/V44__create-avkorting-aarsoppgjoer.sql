CREATE TABLE avkorting_aarsoppgjoer(
    id UUID PRIMARY KEY,
    behandling_id UUID NOT NULL,
    sak_id BIGINT NOT NULL,
    aar SMALLINT NOT NULL
);
ALTER TABLE avkorting_aarsoppgjoer
ADD UNIQUE (behandling_id, aar);

with aktuelle_aarsoppgjoer as (select distinct behandling_id, sakid from avkortingsgrunnlag inner join beregningsperiode on behandlingid = behandling_id)
insert into avkorting_aarsoppgjoer (id, behandling_id, sak_id, aar) select gen_random_uuid() as id, behandling_id, sakid, 2024 from aktuelle_aarsoppgjoer;

-- AVKORTINGSGRUNNLAG
ALTER TABLE avkortingsgrunnlag ADD COLUMN aarsoppgjoer_id UUID;

update avkortingsgrunnlag set aarsoppgjoer_id  = oppgjoer.id
from (select * from avkorting_aarsoppgjoer) oppgjoer
where avkortingsgrunnlag.behandling_id = oppgjoer.behandling_id;

ALTER TABLE avkortingsgrunnlag ALTER COLUMN aarsoppgjoer_id SET NOT NULL;

-- AVKORTINGSPERIODER
ALTER TABLE avkortingsperioder ADD COLUMN aarsoppgjoer_id UUID;

update avkortingsperioder set aarsoppgjoer_id = oppgjoer.id
from (select * from avkorting_aarsoppgjoer) oppgjoer
where avkortingsperioder.behandling_id = oppgjoer.behandling_id;

ALTER TABLE avkortingsperioder ALTER COLUMN aarsoppgjoer_id SET NOT NULL;

-- AVKORTET_YTELSE
ALTER TABLE avkortet_ytelse ADD COLUMN aarsoppgjoer_id UUID;

update avkortet_ytelse  set aarsoppgjoer_id = oppgjoer.id
from (select * from avkorting_aarsoppgjoer) oppgjoer
where avkortet_ytelse.behandling_id = oppgjoer.behandling_id;

ALTER TABLE avkortet_ytelse ALTER COLUMN aarsoppgjoer_id SET NOT NULL;

-- AARSOPPGJOER RESTANSE
ALTER TABLE  avkorting_aarsoppgjoer_restanse ADD COLUMN aarsoppgjoer_id UUID;

update avkorting_aarsoppgjoer_restanse set aarsoppgjoer_id = oppgjoer.id
from (select * from avkorting_aarsoppgjoer) oppgjoer
where avkorting_aarsoppgjoer_restanse.behandling_id = oppgjoer.behandling_id;

ALTER TABLE avkorting_aarsoppgjoer_restanse ALTER COLUMN aarsoppgjoer_id SET NOT NULL;

-- AARSOPPGJOER YTELSE FOER AVKORTING
ALTER TABLE  avkorting_aarsoppgjoer_ytelse_foer_avkorting ADD COLUMN aarsoppgjoer_id UUID;

update avkorting_aarsoppgjoer_ytelse_foer_avkorting set aarsoppgjoer_id = oppgjoer.id
from (select * from avkorting_aarsoppgjoer) oppgjoer
where avkorting_aarsoppgjoer_ytelse_foer_avkorting.behandling_id = oppgjoer.behandling_id;

ALTER TABLE avkorting_aarsoppgjoer_ytelse_foer_avkorting ALTER COLUMN aarsoppgjoer_id SET NOT NULL;
