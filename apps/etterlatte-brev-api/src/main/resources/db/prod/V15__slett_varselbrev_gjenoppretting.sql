-- To journalførte varselbrev men kun et utsendt - Sletter brev som ble journalført uten utsending
insert into hendelse (brev_id, status_id, payload) values (13010, 'SLETTET', 'gjenoppretting');
