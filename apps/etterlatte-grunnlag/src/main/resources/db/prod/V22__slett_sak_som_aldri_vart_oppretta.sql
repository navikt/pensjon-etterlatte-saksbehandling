-- Migrering feila undervegs i transaksjonen vi hadde da som oppretta sak, la inn i grunnlag, oppretta behandling etc.
-- Så da rulla vi tilbake heile transaksjonen med oppretting av sak,
-- men remote-kallet til grunnlag hadde jo allereie gått,
-- så vi fekk lagt inn grunnlag sjølv om saka ikkje fantes.
-- Så vart det oppretta ny sak og alt nytt ved retry, og det gjekk fint.
DELETE FROM behandling_versjon WHERE sak_id = 3881 and behandling_id='20fda915-8d49-41bc-a539-483e8b0474a0';
DELETE FROM grunnlagshendelse WHERE sak_id = 3881;

DELETE FROM behandling_versjon WHERE sak_id = 3882 and behandling_id='a2a999d3-27c3-4441-8e45-1137fcc3291a';
DELETE FROM grunnlagshendelse WHERE sak_id = 3882;