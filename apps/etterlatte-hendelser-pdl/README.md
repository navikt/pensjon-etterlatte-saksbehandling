# Etterlatte-hendelser-pdl

Applikasjonen lytter på livshendelser fra PDL sitt
Kafka-topic `pdl.leesah-v1` ([Livet er en strøm av hendelser](https://pdldocs-navno.msappproxy.net/ekstern/index.html#))
. Hendelser av bestemte typer blir håndtert, og det blir postet events til Rapids & Rivers som plukkes opp av
applikasjonen `etterlatte-oppdater-behandling`.

Bygget med Kotlin, Rapids & Rivers og [Ktor](https://ktor.io/).

## Konsepter

### Livshendelse

Livshendelser representerer endringer som skjer på personene i PDL. Når en person dør, eller en person bytter adresse
kan PDL sende ut livshendelser.

## Flyt

### Livshendelse observeres

Når en livshendelse observeres, sjekkes det hvilken type hendelse det er. Om hendelsen er av en type som kan håndteres,
hentes først fnr for personen hendelsen gjelder fra PDL ved REST-kall til applikasjonen `etterlatte-pdltjenester`.
Videre postes det en melding til Rapids & Rivers med hendelsesnøkkel: `PDL:PERSONHENDELSE`.
Applikasjonen `etterlatte-oppdater-behandling` lytter på disse hendelsene.

## Kom i gang

### Kjøre lokalt

Appen er ikke satt opp for kunne kjøres lokalt p.t.

## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-hendelser-pdl/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
