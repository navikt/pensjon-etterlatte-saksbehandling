
update behandling set status = 'AVBRUTT'
    where status = 'ATTESTERT'
    and sak_id = 12995
    and id ='395c4864-4b04-44c1-9daa-83fe2b8b6c38';

update vedtak
set vedtakstatus = 'RETURNERT',
where sakid = 12995
and behandlingid = '395c4864-4b04-44c1-9daa-83fe2b8b6c38';

