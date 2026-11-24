# Sak og behandlinger

REST-appliksasjon og Kafka-produsent som holder styr på saker, behandlinger og oppgaver.

## Konsepter

### Sak

En sak er et overheng som representerer alt Nav vet eller gjør i forbindelse med en spesifikk ytelse til en spesifikk
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
eksempel være at Nav har mottatt en søknad.

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
for behandlingen vedtakshendelsen gjelder. Det publiseres også vedtakshendelser for eksterne systemer.

### Sjekker om det finnes saker for grunnlagsendringshendelser

Når en grunnlagsendringshendelse er plukket opp i applikasjonen `etterlatte-behandling-kafka` kalles det på
endepunktet `/grunnlagsendringshendelse/{hendelsetype}` hvor `{hendelsetype}` er typen hendelse som er plukket opp.
Her sjekkes det om det finnes en sak tilhørende personen man har mottatt en hendelse for. På nåværende tidspunkt
lyttes det kun til hendelser om dødsfall hos søker.

### Vedtakshendelser for eksterne systemer

Kafka-topic'en **etterlatte.vedtakshendelser** benyttes til dette. Kontakt teamet for tilgang.

Oppdateringer av ACL skjer ikke automatisk ved merge. Kjør GitHub Actions-workflowen
[Deploy Kafka topics](../../../.github/workflows/kafka-topics.yaml) manuelt etter merge for å oppdatere topics i begge clustere.

| context  | manifest                                |
|:---------|:----------------------------------------|
| dev-gcp  | .nais/topic-vedtakshendelser-dev.yaml   |
| prod-gcp | .nais/topic-vedtakshendelser-prod.yaml  |

#### Meldingsinnhold

| Felt        | Datatype   | Beskrivelse                                                             |
|:------------|:-----------|-------------------------------------------------------------------------|
| ident       | String     | Fødselsnummer hendelsen gjelder                                         |
| sakstype    | String     | Sakstype: OMS, BP                                                       |
| type        | String     | Hva vedtaket gjelder: AVSLAG, INNVILGELSE, ENDRING, REGULERING, OPPHOER |
| vedtakId    | Long       | Unik identifikator for vedtaket                                         |
| vedtaksdato | yyyy-mm-dd | Dato vedtaket ble fattet                                                |
| virkningFom | yyyy-mm-dd | Dato vedtaket gjelder fra                                               |

- Se [Vedtakshendelse](./src/main/kotlin/behandling/vedtaksvurdering/outbox/OutboxService.kt) for implementasjon.
- Dette er designet til å gå i tospann med denne [vedtaksinformasjonstjenesten](../etterlatte-api/README.md), dersom en trenger informasjon om utbetalt ytelse.
- **NB:** _REGULERING_ er en spesifikk variant av _ENDRING_. Spesialhåndtert da det vil komme et større volum av denne under den årlige reguleringen.


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

### Aktivering og databasevalg for beregning

`BRUK_INTERN_BEREGNING=true` i behandling kobler beregningstjenestene til de valgte
repositories, registrerer rutene under `/api/beregning` og lar alle interne brukere av
`BeregningKlient` kalle de flyttede tjenestene direkte. Dette gjelder blant annet etteroppgjør,
vedtaksbehandling og dataene til `VedtakOgBeregningSammenligner`. Standardverdien er `false`.
Når bryteren er av, registreres ikke rutene, og de flyttede tjenestene opprettes ikke.
Da bruker interne kall fortsatt `BeregningKlientImpl` mot beregningsappen.
Feil fra de flyttede tjenestene sendes videre uten automatisk forsøk mot den gamle klienten.
`BeregningModule` tilbyr en ikke-null intern klient. `KlientModule` velger mellom intern
klient og HTTP-klient med bryteren. Hvis intern beregning er aktivert uten en konfigurert
intern klient, feiler klientopprettelsen i stedet for å velge HTTP-klienten.
UI-ens BFF har en egen miljøvariabel med samme navn som velger hvilken app forespørslene går til.

Manifestene setter disse verdiene:

| App | Miljøvariabel | Dev | Prod |
| --- | --- | --- | --- |
| Behandling | `BRUK_INTERN_BEREGNING` | `false` | `false` |
| UI | `BRUK_INTERN_BEREGNING` | `false` | `false` |
| Beregning (CRUD-ruter) | `BRUK_INTERN_BEREGNING` | `true` | `false` |
| Behandling | `BRUK_EGEN_DATABASE_FOR_BEREGNING` | `false` | `false` |

`BeregningRepositoryModule` velger database ved oppstart med miljøvariabelen
`BRUK_EGEN_DATABASE_FOR_BEREGNING`. Standardverdien er `false`: når de flyttede tjenestene
aktiveres, går lesinger og skrivinger til CRUD-endepunktene under `/api/beregning/intern`
i `etterlatte-beregning`.
Klienten bruker systemtoken fra behandling. Beregning må ha `BRUK_INTERN_BEREGNING=true`
før disse operasjonene tas i bruk. CRUD-endepunktene er foreløpig deaktivert i prod.

De flyttede tjenestene gjør validering, oppslag og responsmapping før CRUD-skriving der
resultatet ikke avhenger av lagringen. Lokal behandlingsstatus oppdateres før det siste
CRUD-kallet, og transaksjonen holdes åpen til kallet er fullført. Hvis kallet feiler,
rulles lokale statusendringer tilbake. En eventuell transaksjon hos kalleren gjenbrukes.

Etteroppgjør beregner både avkorting og resultat før én
`PUT /api/beregning/intern/etteroppgjoer/{forbehandlingId}` lagrer dem atomisk.
År og avkortingsreferanse kontrolleres før lagring. Beregning må deployes med dette
endepunktet før behandling tar det i bruk. Ingen brytere eller databasedata flyttes av endringen.

Dette gir ikke en felles transaksjon mellom appene: ved timeout etter at beregning har
committet, kan fjernlagringen være gjennomført selv om behandling ruller tilbake.
Den frittstående appen har allerede tilsvarende risiko fordi den lagrer før status
oppdateres over HTTP. Ved uavklart utfall av en beregnings- eller avkortingslagring
gjentas samme steg før arbeidet fortsetter. Dette gjelder ikke automatisk andre
flertrinnsflyter. CRUD-klienten logger operasjon og tilgjengelig behandlings- eller
ressursreferanse, uten payload, og sender feilen videre.
Den samlede repository-operasjonen har også sin egen transaksjon ved lokal JDBC;
den deltar ikke i behandling sin `DatabaseContext`.
Resultatavhengig lokal lagring og hendelser i etteroppgjørsforbehandlingen skjer fortsatt
etter beregningen. For øvrige flertrinnsflyter og gjenstående risiko, se
[migreringsstatusen](../../.github/domenekontekst/beregning-migrering.md).

Hold `BRUK_EGEN_DATABASE_FOR_BEREGNING=false` i prod gjennom CRUD-mellomsteget.
Før lokal database aktiveres må dataflytting og de lokale transaksjonsgrensene avklares.
Med `true` bruker alle seks repositories behandling sin database, uten HTTP-kall.
Endringen krever deployment og omstart. Det finnes ingen automatisk fallback eller
dobbeltskriving; ved bytte må dataene være oppdaterte i databasen som tas i bruk.

Flyttet beregning er deaktivert i både dev og prod. UI bruker fortsatt den frittstående
beregningsappen. Denne endringen klargjør koblingene og databasevalget; aktivering gjøres
i en egen PR. Den eksisterende CRUD-bryteren i beregningsappen endres ikke.
Kafka-konsumenter i andre apper bruker fortsatt beregningsappen.

Ved senere aktivering deployer du først beregning med CRUD-rutene aktivert,
deretter behandling med `BRUK_INTERN_BEREGNING=true` og til slutt UI med samme verdi.
Kontroller at beregningsbryterne i Unleash også gjelder `etterlatte-behandling`.
De flyttede UI-kallene og interne servicekall bruker denne appens Unleash-kontekst.
Etter aktivering kan du teste begge datakildene i dev ved å endre
`BRUK_EGEN_DATABASE_FOR_BEREGNING` i behandling og deploye på nytt.
Bruk testdata som finnes i den valgte databasen,
og kontroller både lagring, uthenting, oppdatering og sletting.
Ved rollback flyttes UI og andre eksterne konsumenter tilbake til beregningsappen først.
For UI setter du `BRUK_INTERN_BEREGNING=false` i BFF-en og deployer UI på nytt.
Ikke deaktiver rutene i behandling mens konsumenter fortsatt sender forespørsler dit.
Deretter kan du sette `BRUK_INTERN_BEREGNING=false` i behandling og deploye på nytt for
å sende interne kall tilbake til beregningsappen. Behold CRUD-rutene i beregningsappen
til ingen gamle instanser eller pågående kall trenger dem.
Med uendret autoritativ database og bakoverkompatible datakontrakter krever dette
ingen datasynkronisering. Rollback av rutingen angrer ikke lagrede endringer.

## Enhetstester

Noen av testene bruker https://www.testcontainers.org/ for å starte en docker-container.
Testene fungerer med Colima som alternativ til docker desktop på mac, men vi har hatt problemer med at docker.sock
blir "borte" (typisk sier testen av det ikke finnes det docker-miljø, mens docker-kommandoer på cli går bra). Følgende
kommando oppretter ny symlink til colima sin docker.sock.

    sudo ln -s /Users/$(whoami)/.colima/docker.sock /var/run/docker.sock 

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

### For Nav-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-etterlatte.
