
update oppgave set status = 'UNDERKJENT' where  sak_id = 12912 and referanse = 'd95be9fc-3acb-462b-96a6-91bac22d2fea';
update oppgave set status = 'UNDERKJENT' where  sak_id = 19447 and referanse = '9987dcf3-61ea-4db8-87a2-7727f06aa75a';
update oppgave set status = 'UNDERKJENT' where  sak_id = 19425 and referanse = '145cb975-c6b9-4138-9fd9-b59a359a6065';

update behandling set status = 'RETURNERT' where sak_id = 12912 and id = 'd95be9fc-3acb-462b-96a6-91bac22d2fea';
update behandling set status = 'RETURNERT' where sak_id = 19447 and id = '9987dcf3-61ea-4db8-87a2-7727f06aa75a';
update behandling set status = 'RETURNERT' where sak_id = 19425 and id = '145cb975-c6b9-4138-9fd9-b59a359a6065';

update vedtak set vedtakstatus = 'RETURNERT' where vedtak.sakid = 12912 and id = 66537;
update vedtak set vedtakstatus = 'RETURNERT' where vedtak.sakid = 19425 and id = 67311;
update vedtak set vedtakstatus = 'RETURNERT' where vedtak.sakid = 19447 and id = 66628;
