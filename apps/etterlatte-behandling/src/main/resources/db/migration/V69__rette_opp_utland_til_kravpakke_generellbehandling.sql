UPDATE GENERELLBEHANDLING set type = 'KRAVPAKKE_UTLAND' where type = 'UTLAND';
UPDATE GENERELLBEHANDLING set innhold = jsonb_set(innhold, '{type}', '"KRAVPAKKE_UTLAND"', true) where innhold -> 'type' is not null;


UPDATE OPPGAVE SET type = 'KRAVPAKKE_UTLAND' where type = 'UTLAND';