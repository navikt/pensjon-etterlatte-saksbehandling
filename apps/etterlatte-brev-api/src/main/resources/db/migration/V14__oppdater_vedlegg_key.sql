with vedlegg_key as (
    select ('{'||index-1||',key}')::text[] as path, vedlegg, brev_id
    from innhold, jsonb_array_elements(innhold.payload_vedlegg::jsonb) with ordinality arr(vedlegg, index)
    where upper(vedlegg->>'key') = 'BEREGNING_INNHOLD'
)
update innhold
set payload_vedlegg = jsonb_set(payload_vedlegg::jsonb, vedlegg_key.path, '"OMS_BEREGNING"', false)::text
from vedlegg_key
where innhold.brev_id = vedlegg_key.brev_id