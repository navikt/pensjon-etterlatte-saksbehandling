UPDATE GENERELLBEHANDLING set type = 'KRAVPAKKE_UTLAND', innhold = jsonb_set(innhold, '{type}', '"KRAVPAKKE_UTLAND"', true) where type = 'UTLAND';

UPDATE OPPGAVE SET type = 'KRAVPAKKE_UTLAND' where type = 'UTLAND';