UPDATE behandling
SET bodd_eller_arbeidet_utlandet = jsonb_set(
        bodd_eller_arbeidet_utlandet - 'vurdereAvoededsTrygdeavtale',
        '{vurdereAvdoedesTrygdeavtale}',
        bodd_eller_arbeidet_utlandet->'vurdereAvoededsTrygdeavtale')
WHERE bodd_eller_arbeidet_utlandet ? 'vurdereAvoededsTrygdeavtale';
