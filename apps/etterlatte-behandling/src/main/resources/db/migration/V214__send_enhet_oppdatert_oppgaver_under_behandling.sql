create table send_melding_om_enhet
(
    oppgave_id uuid,
    referanse  text,
    enhet      text,
    sendt      boolean default false
);

-- Legger inn alle oppgaver knyttet til saksbehandlingsstatistikk-oppgaver som er under behandling
-- i listen over oppgaver som skal bli sendt på nytt
insert into send_melding_om_enhet (oppgave_id, referanse, enhet)
select o.id, o.referanse, s.enhet
from oppgave o
         inner join public.sak s on o.sak_id = s.sak_id
where o.type in ('KLAGE', 'TILBAKEKREVING', 'FOERSTEGANGSBEHANDLING', 'REVURDERING')
  and o.status not in ('AVBRUTT', 'FERDIGSTILT');
