-- Fiks resultat tilbakekreving avbrutt
update sak
set behandling_resultat = 'AVBRUTT'
where behandling_status = 'AVBRUTT'
  and behandling_resultat is null
  and behandling_type = 'TILBAKEKREVING';

-- Fiks registrert tidspunkt klage
update sak
set registrert_tid = foerste_paa_klage.rtid
from (select min(registrert_tid) as rtid, behandling_id
      from sak
      where behandling_type = 'KLAGE'
      group by behandling_id) as foerste_paa_klage
where behandling_id = foerste_paa_klage.behandling_id;

-- Fiks status klager vi har sendt til kabal
-- Ønske om en annen mapping for de som vi sender til kabal
update sak
set behandling_status = 'OVERSENDT_KA'
where behandling_type = 'KLAGE'
  and behandling_status = 'FERDIGSTILT'
  and behandling_resultat = 'STADFESTE_VEDTAK';

-- fix forsvinnende utlandstilknytning revurdering
-- manglende oppdatering i påfølgende meldinger historisk, feilen er fikset men denne patcher
-- gamle saker som ligger i databasen
update sak
set sak_utland = fasit.sak_utland
from (select distinct on (behandling_id) behandling_id, sak_utland from sak
    where sak_utland is not null order by behandling_id, teknisk_tid desc
) as fasit
where behandling_id = fasit.behandling_id
and sak_utland is null;

-- Fjerner en duplisert hendelse på grunn av en bug i ferdigstilling
delete from sak where id = 144748
