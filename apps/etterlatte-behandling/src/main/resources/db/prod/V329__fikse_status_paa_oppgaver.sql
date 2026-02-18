UPDATE oppgave
SET status  = 'UNDER_BEHANDLING',
    merknad = 'Oppgaven ble gjenåpnet fordi behandlingen var åpen'
WHERE id = 'f6a3b9bc-47dc-4860-ace7-c8c714eb7bd4'
  AND sak_id = 15142;
UPDATE oppgave
SET status  = 'UNDER_BEHANDLING',
    merknad = 'Oppgaven ble gjenåpnet fordi behandlingen var åpen'
WHERE id = 'e8ab492f-9173-4eed-a8b3-51780b2fc620'
  AND sak_id = 11865;

