/*
Sak 19640 ble feilaktig opprettet på bruker sitt FNR, når det allerede fantes en pågående sak på DNR.

Overfører brevene tilknyttet den feilopprettede saken (19640) til den opprinnelige (17597).
*/
UPDATE brev
SET sak_id = 17597
WHERE sak_id = 19640;