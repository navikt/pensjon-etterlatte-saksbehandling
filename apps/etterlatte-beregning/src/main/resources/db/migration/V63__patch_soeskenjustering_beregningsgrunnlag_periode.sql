-- En sak i produksjon skal innvilges langt tilbake i tid, og det er ingen søsken.
-- Når vi opprettet beregningsgrunnlaget i saken ble det lagt inn ingen søsken fra original virk,
-- som nå er utdatert. Dette er ikke mulig for saksbehandler å gjøre noe med, siden de ikke får opp valget
-- for søskenjustering.
update beregningsgrunnlag
set soesken_med_i_beregning_perioder = jsonb_set(soesken_med_i_beregning_perioder, '{0,fom}', '"2020-11-01"')
where behandlings_id = '3fec2e07-0839-47f3-b98e-68a99b6a3533';