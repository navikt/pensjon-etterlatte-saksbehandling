/*
Sak 19640 ble feilaktig opprettet på bruker sitt FNR, når det allerede fantes en pågående sak på DNR.
Siden den opprinnelige saken (17597) har iverksatt behandling og pågående utbetaling, gir det mest
mening å beholde denne og la saksbehandler oppdatere ident manuelt.

Overfører oppgaver til opprinnelig sak (17597) og sletter den nye saken (19640) som ikke burde vært opprettet.
*/

-- Overfører 3 oppgaver til den nye saken
UPDATE oppgave
SET sak_id = 17597
WHERE sak_id = 19640;

-- Sletter sak som ikke skulle vært opprettet
DELETE FROM sak WHERE id = 19640;
