-- Sak som har feil enhet og ikke mulig å flytte direkte i Gjenny
UPDATE sak SET enhet = '4817' where id = '16013' AND adressebeskyttelse = 'UGRADERT' AND erskjermet = false;