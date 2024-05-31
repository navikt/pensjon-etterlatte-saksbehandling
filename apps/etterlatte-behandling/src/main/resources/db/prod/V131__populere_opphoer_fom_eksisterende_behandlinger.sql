UPDATE behandling
SET opphoer_fom='"' || subquery.virk || '"'
FROM (
    select id, virkningstidspunkt::jsonb->>'dato' as virk from behandling
    where revurdering_aarsak in ('ADOPSJON','ALDERSOVERGANG','OPPHOER_UTEN_BREV')
    and (opphoer_fom is null or opphoer_fom = 'null')
) AS subquery
WHERE behandling.id = subquery.id;