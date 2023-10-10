alter table generellbehandling add column status TEXT;
UPDATE generellbehandling SET status = 'OPPRETTET'