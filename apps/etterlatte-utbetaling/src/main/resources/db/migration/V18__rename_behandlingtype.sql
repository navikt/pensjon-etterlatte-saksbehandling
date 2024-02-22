UPDATE
    utbetaling
SET vedtak = jsonb_set(vedtak::jsonb,
                       '{behandling,type}', '"FORSTEGANGSBEHANDLING"')::text
WHERE vedtak::json -> 'behandling' ->> 'type' = 'FØRSTEGANGSBEHANDLING';
