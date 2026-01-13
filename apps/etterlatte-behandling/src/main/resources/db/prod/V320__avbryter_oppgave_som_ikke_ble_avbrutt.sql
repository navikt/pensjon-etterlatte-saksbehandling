-- avbryter oppgaven som tilhører tilbakekrevingen riktig denne gangen
UPDATE oppgave
SET status  = 'AVBRUTT',
    merknad = 'Omgjøring av tilbakekreving er avbrutt'
WHERE sak_id = 18981
  AND referanse = '1d7cace7-93b8-404c-8ab6-9b8ac29d476b'
  AND type = 'TILBAKEKREVING'
  AND status = 'ATTESTERING';