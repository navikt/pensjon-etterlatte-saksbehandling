update sak set opprettet_av = 'GJENNY' where opprettet_av is null;
update sak set behandling_metode = 'MANUELL' where behandling_status = 'AVBRUTT' and behandling_metode is null;
