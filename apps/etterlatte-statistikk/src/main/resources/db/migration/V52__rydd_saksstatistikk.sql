-- fiks resultat tilbakekreving
update sak
set behandling_resultat = 'AVBRUTT'
where behandling_status = 'AVBRUTT'
  and behandling_resultat is null
  and behandling_type = 'TILBAKEKREVING'