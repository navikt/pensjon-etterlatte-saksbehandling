/*
Sak 21974 ble feilaktig opprettet på bruker sin nye ident.
Beholder sak 19905 siden den er den opprinnelige saken OG har tilknytning til en oppgave.
*/

-- Sletter sak som ikke skulle vært opprettet
DELETE FROM sak WHERE id = 21974;
