-- Avbryter oppgave feilaktig opprettet for opphør ved alderovergang 21 år
update oppgave set status = 'AVBRUTT' where id = '6169976a-976d-47b1-b8ae-dccb033f0e45';
