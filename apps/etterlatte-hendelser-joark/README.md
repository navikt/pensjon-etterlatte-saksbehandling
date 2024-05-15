# etterlatte-hendelser-joark

Applikasjonen lytter på hendelser fra Joark sitt Kafka-topic `aapen-dok-journalfoering`.
Hendelser med tema **EYO** og **EYB** skal behandles.

Dersom det er en journalpost under behandling eller den mangler sakstilknytning i Gjenny, vil det bli opprettet en
journalføringsoppgave.

## Test i dev

Det finnes flere måter å teste appen på i dev. De enkleste er som følger:

### Metode 1:

> Legg til dokument på testbruker i [Dolly](https://dolly.ekstern.dev.nav.no).

- Velg testbruker
- Klikk `Legg til/endre`
- Velg `Dokumenter`, kryss av valgt type og klikk `Neste`
- Fyll ut skjema og tema (EYO eller EYB). Resten er valgfritt. Om du vil ha en redigerbar journalføringsoppgave må ikke
  journalposten ferdigstilles.
- Last opp dokument og klikk `Neste`
- På siste siden klikker du bare `Opprett`
- Det vil da bli sendt ut en melding på `aapen-dok-journalfoering` som plukkes opp av `etterlatte-hendelser-joark` og
  oppgave blir opprettet.

### Metode 2:

> Last opp dokument i [testdata-frontend](https://testdata-frontend.intern.dev.nav.no)

Denne metoden kan brukes i tilfeller hvor man vil legge til journalpost _uten_ bruker.

Dette er litt mer omfattende siden de ikke har støtte for våre temaer, men enkelt når man først har gjort det én gang.

Del 1:

- Åpne DevTools
- Velg `Sources`
- Finn filen `testdataLanding.js`
- Sett et breakpoint på linjen som sender POST-kall mot `/journalpostapi/v1/journalpost` (opprettelse av journalpost)

Del 2:

- Legg til personnummer
- Velg tema og skjema
- Klikk `Lag journalpost`
- Breakpointet vil nå stoppe prosessen
- Gå i console og skriv `e.tema = 'EYB'`
- Trykk `Resume script execution`
- ... du får nå en ny journalpost og oppgave!

## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-hendelser-joark/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
