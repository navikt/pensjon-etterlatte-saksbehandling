-- Oppgaver ble ikke riktig avbrutt når behandlingen ble det, så vi setter alle oppgaver under arbeid
-- knyttet til behandlinger som nå er avbrutt til å være avbrutt
update oppgave
set status = 'AVBRUTT'
where id in
      (select o.id
       from oppgave o
                inner join behandling b on o.referanse = b.id::text
       where b.status = 'AVBRUTT'
         and o.status in ('NY', 'UNDER_BEHANDLING'));

-- Oppgaver ble opprettet for grunnlagsendringshendelser før de ble sjekket mot tilstanden i grunnlag,
-- som gjorde at noen oppgaver ble opprettet for hendelser som ikke skal behandles
delete
from oppgave
where id in (select o.id
             from oppgave o
                      inner join grunnlagsendringshendelse g on o.referanse = g.id::text
             where g.status = 'FORKASTET');