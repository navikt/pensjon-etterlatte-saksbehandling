-- Tilbakestiller status på behandling hvor vi ikke får fullført attestering
-- fordi behandling allerede har status ATTESTERT
update behandling
set status = 'FATTET_VEDTAK'
where id = '45fa0839-cb54-4559-bc71-bd926889954d'
  and sak_id = 18814
  and status = 'ATTESTERT';
update oppgave
set status        = 'UNDER_ARBEID',
    merknad       = 'Behandling tilbakestilt. ' || merknad
where id = 'e587358e-9d01-416e-b73c-c321f1dde690'
  and sak_id = 18814
  and status = 'FERDIGSTILT';