-- Tilbakestiller status på behandling som er i vranglås opp mot vedtaksstatusen
update behandling
set status = 'BEREGNET'
where id = 'f5c25039-3d89-417b-8775-12982874c8b1'
  and sak_id = 17769
  and status = 'FATTET_VEDTAK';
update oppgave
set status        = 'UNDER_ARBEID',
    saksbehandler = 'J166348',
    merknad       = 'Behandling tilbakestilt. ' || merknad
where id = '675f20e3-13cb-4483-9f90-6226cc4041cc'
  and sak_id = 17769
  and status = 'ATTESTERING';