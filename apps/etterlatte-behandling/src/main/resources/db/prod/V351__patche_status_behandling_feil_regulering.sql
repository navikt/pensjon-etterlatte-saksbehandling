-- Denne behandlingen kom aldri ut til utbetaling. Den ble håndtert i senere kjøring i reguleringen, men
-- akkurat denne behandlingen ble ikke riktig avbrutt. Avbryter den manuelt nå.
update behandling
set status = 'AVBRUTT'
where id = '042e8f6c-135c-44cb-bf11-6ac3041ddb8c'
  and sak_id = 12114
  and status = 'ATTESTERT';
