-- Vi opprettet duplikate oppgaver på meldt inn endring, på grunn av en feil i registrering av at endringen var mottatt.
-- Disse oppgavene har alle samme type og referanse (til journalpost), og vi kan dermed slette alle unntatt en for
-- hver av kombinasjonen sak_id + referanse

delete
from oppgave
where (referanse = '729877139' and sak_id = 25812 and type = 'MELDT_INN_ENDRING' and id != '35c68cba-a7a0-4b57-bcae-f10cc794abb6')
   or (referanse = '729898030' and sak_id = 17471 and type = 'MELDT_INN_ENDRING' and id != '981a8404-07a9-4b0d-8b10-c26dd52f081f')
   or (referanse = '729840564' and sak_id = 21817 and type = 'MELDT_INN_ENDRING' and id != '841d5d0a-6693-47ae-a4f5-5bff93cda1bd')
   or (referanse = '729867166' and sak_id = 22106 and type = 'MELDT_INN_ENDRING' and id != '4e9715f8-b400-42a2-a405-acf1eea5758e')
   or (referanse = '729845396' and sak_id = 21492 and type = 'MELDT_INN_ENDRING' and id != '1756bf27-9fa3-4a0c-8441-40ac54fc7094');

-- Følgende spørring gir treff på en oppgave per sak:
--
-- select id, referanse, sak_id
-- from oppgave
-- where (referanse = '729877139' and sak_id = 25812 and type = 'MELDT_INN_ENDRING' and id = '35c68cba-a7a0-4b57-bcae-f10cc794abb6')
--    or (referanse = '729898030' and sak_id = 17471 and type = 'MELDT_INN_ENDRING' and id = '981a8404-07a9-4b0d-8b10-c26dd52f081f')
--    or (referanse = '729840564' and sak_id = 21817 and type = 'MELDT_INN_ENDRING' and id = '841d5d0a-6693-47ae-a4f5-5bff93cda1bd')
--    or (referanse = '729867166' and sak_id = 22106 and type = 'MELDT_INN_ENDRING' and id = '4e9715f8-b400-42a2-a405-acf1eea5758e')
--    or (referanse = '729845396' and sak_id = 21492 and type = 'MELDT_INN_ENDRING' and id = '1756bf27-9fa3-4a0c-8441-40ac54fc7094');

