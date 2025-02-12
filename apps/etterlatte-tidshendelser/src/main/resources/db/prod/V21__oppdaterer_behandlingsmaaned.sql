-- Korrigerer opprettet jobb etter endring i offset for behandlingsmåned
update jobb set behandlingsmaaned = '2025-02' where id = 205;
