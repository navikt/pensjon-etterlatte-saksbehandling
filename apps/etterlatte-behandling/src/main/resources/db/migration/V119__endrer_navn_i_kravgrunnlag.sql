UPDATE tilbakekreving
SET kravgrunnlag = jsonb_set(kravgrunnlag, '{referanse}', json_build_object(
        'value', kravgrunnlag -> 'sisteUtbetalingslinjeId' ->> 'value')::jsonb, true);