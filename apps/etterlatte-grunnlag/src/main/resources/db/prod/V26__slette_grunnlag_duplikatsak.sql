/*
Sak 21974 ble feilaktig opprettet på bruker sin nye ident.
Grunnlaget i den nyopprettede saken brukes ikke til noe og kan fjernes.
*/
DELETE FROM grunnlagshendelse WHERE sak_id = 21974;

