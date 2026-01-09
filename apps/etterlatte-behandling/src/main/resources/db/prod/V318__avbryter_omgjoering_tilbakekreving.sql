-- Avbryter tilbakekrevingen
UPDATE tilbakekreving
SET status = 'AVBRUTT'
WHERE sak_id = 18981
  AND id = '1d7cace7-93b8-404c-8ab6-9b8ac29d476b';

-- avbryter oppgaven som tilhører tilbakekrevingen
UPDATE oppgave
SET status  = 'AVBRUTT',
    merknad = 'Omgjøring av tilbakekreving er avbrutt'
WHERE sak_id = 18981
  AND referanse = '1d7cace7-93b8-404c-8ab6-9b8ac29d476b'
  AND type = 'TILBAKEKREVING'
  AND status = 'TIL_ATTESTERING';