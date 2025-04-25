UPDATE oppgave SET type = 'OPPFOELGING'
WHERE merknad LIKE 'Sluttbehandling - VO utland'
AND type = 'GENERELL_OPPGAVE';