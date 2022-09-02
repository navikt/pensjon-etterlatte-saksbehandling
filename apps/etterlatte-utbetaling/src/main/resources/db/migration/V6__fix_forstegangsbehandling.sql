UPDATE
    utbetaling SET vedtak = jsonb_set(vedtak::jsonb,
                                        '{behandling,type}' , '"FØRSTEGANGSBEHANDLING"')::text
WHERE
            vedtak::json->'behandling'->>'type' = 'FORSTEGANGSBEHANDLING';
