-- Setter opphoer fra og med på alle opphørsvetdak som ble opprettet før feltet ble innført
UPDATE vedtak
SET opphoer_fom=subquery.datovirkfom
FROM (
    select id, datovirkfom from vedtak
    where type = 'OPPHOER'
    and (opphoer_fom is null)
) AS subquery
WHERE vedtak.id = subquery.id;