UPDATE oppgave SET
   status = 'AVBRUTT',
   merknad = ('Oppgaven er avbrutt på grunn av feil opprettelse. ' || merknad)
where sak_id = 24190
  and id = 'e8088d1a-8a40-47ab-9ded-b9fa4ce828f2'
  and type = 'OMGJOERING'
  and status = 'UNDER_BEHANDLING';

UPDATE oppgave SET
   status = 'AVBRUTT',
   merknad = ('Oppgaven er avbrutt på grunn av feil opprettelse. ' || merknad)
where sak_id = 24190
  and id = '0e9fdf5c-0f5c-42b4-b42c-87df7dd09a4a'
  and type = 'OMGJOERING'
  and status = 'NY';
