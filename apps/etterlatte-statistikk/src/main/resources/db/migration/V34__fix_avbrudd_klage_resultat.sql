UPDATE sak
set behandling_resultat = 'AVBRUTT',
    ferdigbehandlet_tid = registrert_tid
WHERE behandling_type = 'KLAGE'
  and behandling_status = 'AVBRUTT';