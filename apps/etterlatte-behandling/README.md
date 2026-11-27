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

Sett `BRUK_EGEN_DATABASE_FOR_BEREGNING=true` når behandling sin database har de nødvendige
dataene. Da bruker alle seks repositories behandling sin database, uten HTTP-kall.
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
For å sende UI-trafikken tilbake til beregningsappen setter du UI-ens
`BRUK_INTERN_BEREGNING=false` og deployer UI på nytt. Ikke deaktiver rutene i behandling
mens UI fortsatt sender forespørsler dit.
Når UI er flyttet tilbake, kan du også sette `BRUK_INTERN_BEREGNING=false` i behandling
og deploye behandling på nytt for å sende interne kall tilbake til beregningsappen.

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

## Masseutsending av informasjonsbrev

Batch-API-et tar imot opptil 1 000 unike sak-ID-er, en felles tittel, Slate-tekst og ett språk.
Timerjobben behandler én sak om gangen. Den oppretter ikke behandlinger og endrer ikke vedtak.
Innsenderen må ha godkjent teksten og sakslisten faglig før innsending. Gjenny har ingen forhåndsvisning eller godkjenning for denne flyten.

### Opprette og følge en batch

Bruk et systembrukertoken fra den eksplisitt tillatte klienten. Eksemplet har fiktive sak-ID-er:

```http
POST /api/brev/batcher
Content-Type: application/json
Idempotency-Key: 90ca626c-c776-4e4a-923e-f828dc9ea188
Authorization: Bearer <systembrukertoken>

{
  "sakIder": [100001, 100002],
  "tittel": "Informasjon om din ytelse",
  "innhold": [
    {
      "type": "paragraph",
      "children": [{"text": "Dette brevet er til informasjon. Du trenger ikke svare."}]
    }
  ],
  "spraak": "nb"
}
```

Svaret er `202 Accepted`, med batch-ID og statusadresse i `Location`.
Les oversikten med `GET /api/brev/batcher/{batchId}`, og resultater per sak med
`GET /api/brev/batcher/{batchId}/saker?offset=0&limit=100`. Bare batchens tillatte innsender kan lese resultatene.

Hvis svaret på opprettelsen uteblir, send samme request med samme `Idempotency-Key`.
Samme innsender, nøkkel og innhold gir samme batch. Sak-ID-er dedupliseres og sorteres.
Endret saksliste, tittel, tekst eller språk med samme nøkkel gir `409 Conflict`.
En ny nøkkel betyr en ny utsending, også til saker som allerede har fått teksten.

Requesten kan være opptil 256 KiB, tittelen opptil 200 tegn og samlet tekst opptil 50 000 tegn.
Slate kan ha opptil 2 000 noder og åtte nivåer. HTML, maluttrykk og uutfylte tekstfelt avvises.
Språkverdiene i JSON er `nb`, `nn` og `en`. Sakens registrerte språk må stemme med batchens språk.
Ukjent språk, uavklart verge eller manglende grunnlag gir feil på den aktuelle saken. De andre sakene fortsetter.
Tekst, språk og datagrunnlag lagres før første brevkall og gjenbrukes ved gjenopptakelse.

### Konfigurasjon og stopp

Funksjonen er avslått som standard.

| Miljøvariabel | App | Standard |
| --- | --- | --- |
| `BREV_BATCH_AKTIVERT` | behandling og brev-api | `false` |
| `BREV_BATCH_TILLATT_INNSENDER` | behandling | Tom, ingen innsender er tillatt |
| `BREV_BATCH_TILLATT_BEHANDLING` | brev-api | Tom, ingen intern klient er tillatt |
| `BREV_BATCH_INTERVALL_SEKUNDER` | behandling | `10` |
| `BREV_BATCH_MAKS_FORSOEK` | behandling | `3` |

Tillatt identitet sammenlignes med `azp_name` i det validerte systembrukertokenet, ikke med requestdata.
Avklar riktige identiteter for miljøet før aktivering. Ingen produksjonsidentiteter er lagt inn som standard.
Sett `BREV_BATCH_AKTIVERT=false` og deploy for å stanse nye batcher og nye utsendinger.
Ventende saker beholdes; et eksternt kall som allerede har startet, kan fortsatt fullføres.

### Feil og uavklarte utsendinger

`FERDIG` betyr journalført brev og akseptert distribusjonsbestilling for alle mottakere, ikke bekreftet levering.
`FEILA` avslutter saken med en feilkode. `AVVENTER_AVKLARING` betyr at et avbrutt eksternt kall kan ha lykkes.
Jobben bruker samme utsendings-ID, datagrunnlag og brev-ID ved gjenopptakelse.
Den gjentar ikke blindt et avbrutt kall under journalføring eller distribusjon.

Ved `AVVENTER_AVKLARING` må operatøren undersøke utsendingsstatus i brev-api, Joark og Brevdistribusjon før retting.
Ikke opprett en ny batch eller nullstill et påbegynt steg for å forsøke på nytt.
Første versjon har ikke et API for operatøravklaring. Ikke legg token, personopplysninger eller brevtekst i logger.

Før produksjonsaktivering må teamet bekrefte faktisk PDF-rendering av `TOM_MAL_INFORMASJONSBREV`,
valgt språk, mottakerregler og automatisk avsender. Kjør databasetestene og test avbrudd mellom eksternt kall og lokal lagring.
Lokale idempotensnøkler er ikke bevis på «exactly once» hos eksterne tjenester.
