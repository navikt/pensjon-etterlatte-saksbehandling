/* for oppgaver med merknad og kommentar */
UPDATE oppgave
SET merknad = REPLACE(merknad, '.. Kommentar: ', '. Kommentar: ')
WHERE merknad LIKE '%.. Kommentar: %';

/* for oppgaver med kun kommentar */
UPDATE oppgave
SET merknad = REPLACE(merknad, '. Kommentar: ', 'Kommentar: ')
WHERE merknad LIKE '. Kommentar: %';