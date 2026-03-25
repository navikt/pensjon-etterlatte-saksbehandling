update tilbakekreving
set status = 'UNDERKJENT'
where id = '3f911dc0-486f-4516-87ea-a55c31dfe013'
  and status = 'FATTET_VEDTAK';

update oppgave
set status = 'UNDERKJENT', merknad = 'Tilbakekreving underkjent'
where type = 'TILBAKEKREVING'
  and status = 'ATTESTERING'
  and referanse = '3f911dc0-486f-4516-87ea-a55c31dfe013';