
-- Det ble opprettet aktivitetspliktoppgaver 12 måneder for feil måneder i starten av februar.
-- Avbryter disse rett i databasen, siden det ikke er noe andre system vi har sendt noe om disse oppgavene
update oppgave
set (status, merknad) = ('AVBRUTT', 'Automatisk avbrutt på grunn av feil i oppretting')
where opprettet >= '2025-02-01'
  and opprettet <= '2025-02-04'
  and status in ('NY', 'UNDER_BEHANDLING')
  and type = 'AKTIVITETSPLIKT_12MND'
