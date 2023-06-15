# Sak og behandlinger

REST-appliksasjon og Kafka-produsent som holder styr på saker, behandlinger og oppgaver.

## Konsepter

### Sak

En sak er et overheng som representerer alt NAV vet eller gjør i forbindelse med en spesifikk ytelse til en spesifikk
person. Én person har et unikt saksnummer for en spesifikk ytelse.

### Behandling

En behandling representerer at noe skal gjøres knyttet til en sak. En behandling gjelder kun en sak, men det kan gjøres
mange behandlinger i samme sak.
Et eksempel på behandling er førstegangsbehandling av søknad om barnepensjon, eller en revurdering ved endring av
grunnlag som har innvirkning på ytelsen.

En Behandling starter når det er identifisert et behandlingsbehov. En behandling innebærer

1. Motta behandlingsbehov
2. Lagre persongalleri og gyldighetprøving
3. Lytte og sende ut melding om grunnlaget på en sak endrer seg

### Behandlingsbehov

Et behandlingsbehov representerer at det har skjedd noe som gjør at det må gjøres en behandling i en sak. Det kan for
eksempel være at NAV har mottatt en søknad.

### Oppgave

En oppgave representerer noe en saksbehandler må gjøre i forbindelse med en sak.

### Grunnlagsendringshendelse

En grunnlagsendringshendelse representerer en endring i grunnlag i en sak. Hendelsen er typisk en livshendelse som er
plukket opp av leesah-topicen i applikasjonen `etterlatte-hendelser-pdl`. En grunnlagsendringshendelse kan potensielt
føre til en revurdering dersom endringen påvirker vilkårsvurderingen for ytelsen.

## Flyt

### Opprette sak

En ny sak blir opprettet ved et `GET`-kall til `/personer/{id}/saker/{type}` hvor `id` er fødselsnummer og `type`
er `BARNEPENSJON` eller `OMSTILLINGSSTOENAD`.

### Behandlinger opprettes

Nye behandlinger opprettes ved kall på REST-endepunkter som finnes under `BehandlingRoutes.kt` - avhengig av hvilken
type behandling som skal opprettes. Eksempler er Førstegangsbehandling, revurdering og manuelt opphør. De ulike
endepunktene oppretter behandlinger av rett type som skrives til databasen. Det postes så en hendelse til Rapids &
Rivers med hendelsesnøkkel `BEHANDLING:OPPRETTET`.

### Registrer vedtakshendelser

Hendelser om vedtak påvirker statusen til en behandling. Når en vedtakshendelse mottas lagres den oppdaterte statusen
for behandlingen vedtakshendelsen gjelder.

### Sjekker om det finnes saker for grunnlagsendringshendelser

Når en grunnlagsendringshendelse er plukket opp i applikasjonen `etterlatte-oppdater-behandling` kalles det på
endepunktet `/grunnlagsendringshendelse/{hendelsetype}` hvor `{hendelsetype}` er typen hendelse som er plukket opp.
Her sjekkes det om det finnes en sak tilhørende personen man har mottatt en hendelse for. På nåværende tidspunkt
lyttes det kun til hendelser om dødsfall hos søker.

### Jobb for dødshendelser

For å unngå å agere på dødshendelser med en gang, i tilfelle korreksjoner skal komme på pdl-topicen (Livet er en
strøm av hendelser) lagres mottatte dødshendelser ned uten å agere på de. Det kjøres da en jobb med frekvens som
definert i miljøvariabel `HENDELSE_JOB_FREKVENS` som sjekker om hendelser som er `HENDELSE_MINUTTER_GAMLE_HENDELSER` (
miljøvariabel) minutter gamle fortsatt er korrekte. Hvis opplysningene i hendelsen fortsatt stemmer opprettes en
revurdering automatisk, dersom det ikke finnes en åpen behandling på saken. Dersom det finnes en åpen behandling, gjøres
informasjonen tilgjengelig under endepunktet `/grunnlagsendringshendelse/{sak}/allehendelser` - hvor {sak} er
saksnummer.

## Kom i gang

### Hvordan kjøre lokalt mot dev-gcp

Les [README](../../README.md) på rot i prosjektet.

...og legg til følgende linje nederst i `.env.dev-gcp` fila til saksbehandling-ui.

```
BEHANDLING_API_URL=http://host.docker.internal:8090
```

## Bygg og deploy

Appen bygges og deployes automatisk ved commits til `apps/etterlatte-behandling/**`.

For å trigge **manuell deploy** kan du gå til `Actions -> (velg workflow) -> Run workflow from <branch>`

## Enhetstester

Noen av testene bruker https://www.testcontainers.org/ for å starte en docker-container.
Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock
blir "borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende
kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock 

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #po-pensjon-team-etterlatte.
