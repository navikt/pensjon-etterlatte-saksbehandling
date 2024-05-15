-- patcher til strukturert resultat for to rader som har fått feil
UPDATE sak set behandling_resultat = 'INGEN_TILBAKEKREV' where id = 68729;
UPDATE sak set behandling_resultat = 'INGEN_TILBAKEKREV' where id = 68417;