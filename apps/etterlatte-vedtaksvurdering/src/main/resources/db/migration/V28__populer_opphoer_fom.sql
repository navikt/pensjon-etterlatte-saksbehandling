UPDATE vedtak
SET opphoer_fom=subquery.datovirkfom
FROM (
    select id, datovirkfom from vedtak
    where type = 'OPPHOER'
    and (opphoer_fom is null)
) AS subquery
WHERE vedtak.id = subquery.id;