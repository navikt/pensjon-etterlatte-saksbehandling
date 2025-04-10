UPDATE oppgave
SET merknad = REPLACE(merknad, '. Kommentar: ', 'Kommentar: ')
WHERE merknad LIKE '. Kommentar: %';