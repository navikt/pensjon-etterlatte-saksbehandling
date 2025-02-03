update revurdering_info
set info = jsonb_set(info, '{type}', '"SLUTTBEHANDLING"'::JSONB)
where info->>'type' = 'SLUTTBEHANDLING_UTLAND';