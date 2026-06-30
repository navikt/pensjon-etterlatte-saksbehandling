
update behandling
    set status = 'AVBRUTT',
        aarsak_til_avbrytelse = 'AVBRUTT_PAA_GRUNN_AV_FEIL',
        kommentar_til_avbrytelse = 'Avbrutt på grunn av feil i regulering'
    where status = 'ATTESTERT'
    and sak_id = 12995
    and id = '395c4864-4b04-44c1-9daa-83fe2b8b6c38';

update vedtak set vedtakstatus = 'RETURNERT'
    where vedtakstatus = 'ATTESTERT'
    and sakid = 12995
    and behandlingid = '395c4864-4b04-44c1-9daa-83fe2b8b6c38';

