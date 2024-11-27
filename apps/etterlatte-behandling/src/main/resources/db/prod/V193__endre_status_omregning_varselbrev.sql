UPDATE omregningskjoering
SET status = 'TIL_MANUELL_UTEN_OPPGAVE'
WHERE id in ('fe19600c-10b0-474a-89e8-0c9834349016', '0ded68f7-bdf9-4f61-addb-7f7704268dce')
  AND status = 'TIL_MANUELL';