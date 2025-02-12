/*
Sak 19640 ble feilaktig opprettet på bruker sitt FNR, når det allerede fantes en pågående sak på DNR.

Grunnlaget i den nyopprettede saken brukes ikke til noe og kan fjernes.
*/
DELETE FROM grunnlagshendelse WHERE sak_id = 19640;