UPDATE aktivitetsplikt_unntak
SET fom = subquery.opprettet_date,
    beskrivelse = beskrivelse || ' - Fra-dato ble automatisk satt av Gjenny -'
FROM (
         SELECT CAST(opprettet::jsonb ->>'tidspunkt' AS DATE) AS opprettet_date, id
         FROM aktivitetsplikt_unntak
         WHERE fom IS NULL
     ) AS subquery
WHERE aktivitetsplikt_unntak.id = subquery.id;