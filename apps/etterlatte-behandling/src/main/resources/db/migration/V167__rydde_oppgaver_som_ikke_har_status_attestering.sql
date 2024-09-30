-- Siden ta av vent-jobben kan ha satt en attesteringsoppgave som var på vent tilbake til under behandling
-- er det ~10 oppgaver som har fått feil status i basen (etter manuell patching av to av de)
--
-- Tok en kontroll av de 6 oppgavene som var på vent som hadde behandling til attestering, og
-- ingen av de vil få feil status når de tas av vent (verifisert ved å kontrollere oppgaveendringer
-- og dobbeltsjekke at siste status før på vent er attestering
--
-- spørring for å sjekke i opppgaveendringer:
-- select id, oppgaveid, oppgavefoer::jsonb ->> 'status', oppgaveetter::jsonb ->> 'status', tidspunkt
-- from oppgaveendringer
-- where oppgaveid in <AKTUELLE_OPPGAVER>
-- order by oppgaveid, tidspunkt;
update oppgave
set status = 'ATTESTERING'
where id in (SELECT o.id
             FROM oppgave o
                      left outer join behandling b on o.referanse = b.id::text
             where b.status = 'FATTET_VEDTAK'
               and o.status = 'UNDER_BEHANDLING');
