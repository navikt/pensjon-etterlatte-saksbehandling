UPDATE behandling
SET bodd_eller_arbeidet_utlandet = (
    jsonb_set(
            (bodd_eller_arbeidet_utlandet::jsonb - 'vurdereAvoededsTrygdeavtale'),
            '{vurdereAvdoedesTrygdeavtale}',
            (bodd_eller_arbeidet_utlandet::jsonb -> 'vurdereAvoededsTrygdeavtale')
    )::text
)
WHERE bodd_eller_arbeidet_utlandet IS NOT NULL
  AND (bodd_eller_arbeidet_utlandet::jsonb ? 'vurdereAvoededsTrygdeavtale');
