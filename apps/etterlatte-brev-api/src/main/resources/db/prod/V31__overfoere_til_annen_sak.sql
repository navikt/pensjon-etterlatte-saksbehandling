/*
Sak 21974 ble feilaktig opprettet på bruker sin nye ident.
Overfører brev som ble opprettet på den nye saken til den gamle.
*/

UPDATE brev
SET sak_id = 19905
WHERE sak_id = 21974;