-- Setter oppgaver til alle feilede jobber til avbrytt
-- Se V16__kjoerer_AO_BP_20_paa_nytt_igjen.sql (tidshendelser)
update oppgave
set status  = 'AVBRUTT',
    merknad = 'Avbrutt på grunn av bug under automatisk jobb'
where sak_id in (select id
                 from oppgave
                 where status = 'NY'
                   and opprettet >= '2024-10-21'
                   and opprettet < '2024-10-22'
                   and merknad = 'Aldersovergang barnepensjon ved 20 år'
                   and type = 'REVURDERING')