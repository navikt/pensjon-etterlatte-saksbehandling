UPDATE avstemming avs
SET loepende_utbetalinger = (
    SELECT jsonb_agg(
        jsonb_set(
            ub.value,
            '{utbetalingslinjer}',
            (
                SELECT jsonb_agg('{"kjoereplan": "N"}' || ubl)
                FROM jsonb_array_elements(ub.value -> 'utbetalingslinjer') ubl
            )
        )
    )::text FROM jsonb_array_elements(avs.loepende_utbetalinger::jsonb) ub
);