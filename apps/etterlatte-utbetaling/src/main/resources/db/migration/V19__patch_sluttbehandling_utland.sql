UPDATE
    utbetaling SET vedtak = jsonb_set(vedtak::jsonb,
                                      '{behandling,revurderingsaarsak}' , '"SLUTTBEHANDLING"')::text
WHERE
    vedtak::json->'behandling'->>'revurderingsaarsak' = 'SLUTTBEHANDLING_UTLAND';

UPDATE
    utbetaling SET vedtak = jsonb_set(vedtak::jsonb,
                                      '{behandling,revurderingInfo,type}' , '"SLUTTBEHANDLING"')::text
WHERE
    vedtak::json->'behandling'->'revurderingInfo'->>'type' = 'SLUTTBEHANDLING_UTLAND';

