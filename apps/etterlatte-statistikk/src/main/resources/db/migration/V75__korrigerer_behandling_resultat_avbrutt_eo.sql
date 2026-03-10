update sak
set behandling_resultat = 'AVBRUTT'
where behandling_type = 'ETTEROPPGJOER_FORBEHANDLING'
  and (behandling_resultat is null or behandling_resultat = 'null')
  and behandling_status = 'AVBRUTT';