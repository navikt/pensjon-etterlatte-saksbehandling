UPDATE
    utbetaling SET vedtak = jsonb_set(vedtak::jsonb,
                                      '{behandling,revurderingsaarsak}' , '"SLUTTBEHANDLING"')::text
WHERE
    vedtak::json->'behandling'->>'revurderingsaarsak' = 'SLUTTBEHANDLING_UTLAND';
