/* Oppgave ikke ferdigstilt fordi behandling ble avbrutt av annen enn eier av oppgaven
   Ref https://jira.adeo.no/browse/FAGSYSTEM-357812
 */
UPDATE oppgave SET status = 'FERDIGSTILT' where id = '50c6f3a7-0fca-40d1-8498-e68c604af08d';

/* Denne oppgavens referanse peker på siste behandling (når oppgaven ble opprettet). Dette gir ingen mening da det var
   en regulering og denne oppgaven opprettes fordi det feilet å lage en opphørsbehandling.
   Saken er nå løst i en anne behandling så oppgaven skal derfor ferdigstilles.

   Referanse når disse oppgavene opprettes skal bare være tom. Dette er løst her:
   https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/6417

   Se også porten-sak: https://jira.adeo.no/browse/FAGSYSTEM-357812
 */
UPDATE oppgave SET status = 'FERDIGSTILT', referanse = '' where id = '485ac2f4-95b9-4e54-941a-764c3519e97a';