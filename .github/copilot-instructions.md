# Copilot-instruksjoner

## Oversikt

Monorepo for Navs saksbehandlingssystem for etterlatteytelser, som dekker to ytelsestyper:
- **Barnepensjon (BP)**
- **Omstillingsstønad (OMS)**

Systemet heter **Gjenny** internt.

## Sentrale domenebegreper

- **Sak** = én ytelsestype for én person (unik per person + saktype)
- **Behandling** = én endringsbehandling på en sak (`FØRSTEGANGSBEHANDLING` eller `REVURDERING`)
- **Vedtak** = det juridiske resultatet av en behandling (innvilgelse, avslag, opphor, endring)
- **Iverksetting** = utføring av vedtak: utbetaling opprettes, brev sendes, eksterne systemer varsles
- **Omregning** = automatisk revurdering (G-regulering, årlig inntektsjustering, aldersovergang)
- **Etteroppgjør** = årlig oppgjør etter skattefastsetting (kun OMS) – to steg: varselbrev + revurdering

### BP vs. OMS

- **BP** er enklere: ren beregning uten inntektsjustering
- **OMS** har mer kompleksitet: avkorting (inntektsreduksjon), aktivitetsplikt, etteroppgjør, samordning mot Pesys

## Arkitektur

### Apper (`apps/`)
~25 Kotlin/Ktor-backend-mikrotjenester + én React/TypeScript-frontend:

- **`etterlatte-behandling`** – kjerne-saksbehandlingstjeneste. Mesteparten av domenelogikken lever her, inkludert vilkårsvurdering.
- **`etterlatte-*-kafka`** – separate apper som konsumerer meldinger fra Rapids-en og kaller tilhørende REST-app. Mønsteret sikrer at en hengende kafka-melding ikke låser ned selve REST-tjenesten. Gjelder `beregning`, `trygdetid`, `vedtaksvurdering`, `behandling`, `brev` og `vilkaarsvurdering`.
- **`etterlatte-saksbehandling-ui`** – React-frontend med Node/Express som BFF (backend-for-frontend)
- **`etterlatte-api`** – eksternt API for tjenestepensjonsleverandører (TP-ordninger) og andre Nav-team. Ikke det samme som frontendens BFF.
- **`etterlatte-pdltjenester`** – proxy mot PDL (Folkeregisteret). Persondata som er avgjørende for vedtaksdokumentasjon lagres i grunnlagshendelse-tabellen i `etterlatte-behandling` sin database.
- **`etterlatte-tidshendelser`** – trigger tidsbaserte hendelser som aldersoverganger, G-regulering og årlig inntektsjustering, publisert på Rapids-en.
- **`etterlatte-klage`** – proxy mot Kabal (klageenhetens saksbehandlingssystem). Ingen egen database.
- **`etterlatte-tilbakekreving`** – proxy mot tilbakekrevingskomponenten. Har database for å kvittere på sendte og mottatte meldinger.
- **`etterlatte-statistikk`** – henter data fra BigQuery og deler saksbehandlings- og stønadsstatistikk med interne team via Metabase/BigQuery.
- **`etterlatte-egne-ansatte-lytter`** – lytter på Kafka-topic for Nav-ansatte og familiemedlemmer («egne ansatte») slik at sakene rutes til saksbehandlere med særskilte tilganger.
- **`etterlatte-institusjonsopphold`** – lytter på hendelser om institusjonsopphold (tilsvarende hendelser-appene).
- **`etterlatte-hendelser-pdl/joark/samordning/ufoere`** – broer mellom eksterne Kafka-topics og Gjennys interne Rapids. Oversetter eksterne hendelser til interne meldingsformater.
- Domenespesifikke tjenester: `etterlatte-beregning`, `etterlatte-vedtaksvurdering`, `etterlatte-trygdetid`, `etterlatte-brev-api`, `etterlatte-utbetaling` – se [domenekontekst](#domenekontekst).

### Biblioteker (`libs/`)
Delte Kotlin-biblioteker. Konvensjon: `*-model`-biblioteker inneholder kun dataklasser/DTO-er som deles på tvers av tjenester. Andre biblioteker inneholder delt infrastruktur (database, ktor-oppsett, kafka, logging osv.).

### Kommunikasjonsmønstre
- REST mellom tjenester (Ktor-klienter med Azure AD / on-behalf-of-autentisering)
- Kafka via Nav Rapids & Rivers for asynkron hendelseshåndtering (River = Kafka-meldingshåndterer)
- **Rapid** (intern topic) brukes for kommunikasjon mellom Gjennys egne tjenester
- **`etterlatte.vedtakshendelser`** er et separat topic beregnet for deling med andre systemer i Nav (f.eks. TP-ordninger)
- Maskin-til-maskin-kall bruker `httpClientClientCredentials` fra `libs/etterlatte-ktor`
- On-behalf-of-kall (brukerflyt) bruker `DownstreamResourceClient` fra `libs/etterlatte-ktor`

## Bygg og test

### Forutsetninger
- Docker må kjøre (tester bruker Testcontainers for Postgres)
- Sett miljøvariabelen `GITHUB_TOKEN` (PAT med `read:packages`) for å hente interne GitHub-pakker

### Gradle (backend)
```sh
# Bygg alt
./gradlew build

# Test alt
./gradlew test

# Test ett enkelt modul
./gradlew :apps:etterlatte-behandling:test

# Test én enkelt testklasse
./gradlew :apps:etterlatte-behandling:test --tests "no.nav.etterlatte.behandling.BehandlingServiceTest"

```

Backend-linting (ktlint) kjøres automatisk via Husky pre-commit hook – det finnes ingen Gradle-task for ktlint. ktlint installeres via `brew install ktlint`.

### Frontend (`apps/etterlatte-saksbehandling-ui/`)
`client` og `server` er separate pakker med hvert sitt `yarn`-oppsett.

```sh
# Installer avhengigheter (kjøres i client/ eller server/)
yarn

# Start frontend-utviklingsserver (fra client/)
yarn dev

# Kjør tester (fra client/)
yarn test

# Kjør én enkelt testfil (fra client/)
yarn test src/path/to/file.test.ts

# Bygg (fra client/)
yarn build

# Lint (fra client/)
yarn lint
```

## Domenekontekst

For mer detaljert kontekst om enkeltapper, se:

- [saksgangen](domenekontekst/saksgangen.md) – fullstendig livssyklus for en sak, fra søknad til iverksatt vedtak
- [beregning](domenekontekst/beregning.md) – beregner ytelsesbeløp for BP og OMS
- [trygdetid](domenekontekst/trygdetid.md) – opptjeningsperioder som grunnlag for beregning
- [utbetaling](domenekontekst/utbetaling.md) – oversetter vedtak til utbetalinger via Oppdragssystemet
- [brev-api](domenekontekst/brev-api.md) – genererer og distribuerer brev til brukere
- [vedtaksvurdering](domenekontekst/vedtaksvurdering.md) – oppretter og forvalter vedtak, orkestrator mot nedstrøms systemer
- [etteroppgjoer](domenekontekst/etteroppgjoer.md) – årlig etteroppgjør for OMS: flyt, statusmaskin og nøkkelklasser

## Viktige konvensjoner

**`etterlatte-behandling`** er autoriteten på tilgang i Gjenny. De andre appene sjekker tilgang ved å kalle behandling, ikke ved å tolke tokens lokalt. `Kontekst`-objektet (tråd-lokal brukerinfo) er spesifikt for `etterlatte-behandling` og gjenfinnes ikke i de øvrige appene.

Grunnlag (persondata og opplysninger knyttet til sak/behandling) og vilkårsvurdering var tidligere egne tjenester, men er begge slått sammen med `etterlatte-behandling`. `GrunnlagKlient` i andre apper peker derfor nå mot `etterlatte-behandling`. `etterlatte-vilkaarsvurdering-model` eksisterer fortsatt som separat lib siden vilkårsvurdering er et eget domenekonsept, og `etterlatte-vilkaarsvurdering-kafka` gjenstår ennå som egen kafka-app.

### Dependency injection (Kotlin)
Inget DI-rammeverk brukes. Hver app har en `ApplicationContext`-klasse (i `config/`) som manuelt kobler sammen alle avhengigheter. Tjenester, DAO-er og klienter er properties på denne klassen. Tester overstyrer spesifikke konstruktørparametere for å injisere mock-er/stub-er.

### Database (Kotlin)
- Flyway-migreringer ligger i `src/main/resources/db/migration/`
- Rå JDBC via `kotliquery` (ikke ORM). DAO-er tar imot `DataSource` eller `ConnectionAutoclosing`
- DB-tester bruker `@ExtendWith(DatabaseExtension::class)`, som starter én delt Testcontainers Postgres-instans. Hver app definerer sin egen `DatabaseExtension` med `@ResetDatabaseStatement` som trunkerer tabeller mellom testklasser.

### Kafka Rivers (Kotlin)
Meldingshåndterere extender `ListenerMedLogging` og kaller `initialiserRiver(...)` i `init`. Handleren mottar en `JsonMessage`-pakke og implementerer `haandterPakke(packet, context)`.

### Gradle convention plugins
Apper bruker plugins definert i `buildSrc/src/main/kotlin/`:
- `etterlatte.common` – basis Ktor-appoppsett (stdlib, logging, metrikker, testavhengigheter, fat jar)
- `etterlatte.postgres` – legger til PostgreSQL, Flyway, HikariCP og Testcontainers

Avhengighetsversjoner styres i `gradle/libs.versions.toml` (version catalog).

### Frontend API-lag
Alle API-kall går gjennom `apiClient` (`src/shared/api/apiClient.ts`), som wrapper `fetch` og returnerer `ApiResponse<T>` (enten `ApiSuccess<T>` eller `ApiError`).

Typen `Result<T>` (`src/shared/api/apiUtils.ts`) med tilstandene `initial | pending | success | error` brukes til å håndtere asynkron tilstand i komponenter via `useApiCall`-hooken.

Feature toggles er deklarert i `FeatureToggle`-enumen i `src/useUnleash.ts`.

### Frontend-stialias
`~` er mappet til `src/` (konfigurert i `tsconfig.json`). Bruk `~shared/...`, `~components/...`, osv.

### Navngiving
Norsk brukes gjennomgående: variabelnavn, funksjonsnavn, kommentarer og testbeskrivelser er ofte på norsk. Domeneterm som `sak`, `behandling`, `saksbehandler`, `vedtak`, `trygdetid` og `beregning` går igjen i hele kodebasen.

### Lokal utvikling
1. Kjør `bash get-started.sh` én gang for å installere Husky, Prettier og ktlint-hooks
2. Kjør `get-secret.sh` for å hente secrets til appen du vil kjøre
3. Kjør `docker compose up -d` fra app-mappen
4. Bruk IntelliJ `.run`-konfiger (krever [EnvFile-pluginen](https://plugins.jetbrains.com/plugin/7861-envfile))

For Colima (i stedet for Docker Desktop):
```sh
export DOCKER_HOST=unix://${HOME}/.colima/default/docker.sock
export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
```


## Ufravikelige regler

Følgende regler skal **aldri** brytes, uansett kontekst:

1. **Flyway-migrasjoner som er kjørt i produksjon skal aldri endres** – opprett nye migrasjoner for å rette feil
2. **Ingen app skal skrive direkte til en annen apps database** – all kommunikasjon skjer via REST eller Kafka
3. **Iverksatte eller attesterte behandlinger skal aldri endres** – opprett ny revurdering i stedet
4. **`etterlatte-behandling` er autoriteten for tilgangskontroll** – andre apper kaller behandling for å sjekke tilgang
5. **Grunnlagsdata skal hentes fra `etterlatte-behandling`** via `GrunnlagKlient`, ikke direkte fra PDL eller andre kilder
6. **Vedtak skal ikke fattes med kjente feil** – hvis systemet oppdager en inkonsistens som vil gi feil vedtak, skal saksbehandler informeres og blokkeres fra å gå videre til feilen er løst. Dette er et lovkrav, ikke en designpreferanse.

## Designprinsipper

Disse prinsippene styrer løsningsvalg og bør veie tungt ved sparring:

- **Saksbehandler i førersetet for vedtaksdata.** Systemet skal ikke automatisk transformere eller avlede sentrale data som inngår i vedtak (beregningsgrunnlag, inntekt, perioder). Når en feil eller inkonsistens oppdages, skal saksbehandler eksplisitt oppgi korrekte verdier — systemet tilbyr et grensesnitt for å løse det, ikke en autokorrigering. Automatiserte beregninger er akseptabelt *kun* der logikken er eksplisitt dokumentert gjennom regel-biblioteket.

- **Detekter og håndter feil på det tidligste punktet der du har komplett kunnskap.** En kjent feil bør ikke passere videre i flyten. Finn det steget der (a) all nødvendig informasjon er tilgjengelig og (b) saksbehandler faktisk kan handle — og plasser sperren der.

## Teknisk gjeld å kjenne til

- **brev-api** har gammel brevflyt (brev-api bygger brevdata selv) vs. ny strukturert brevflyt (behandling bygger brevdata). Nye brevtyper skal alltid bruke ny flyt. Unngå endringer i gammel flyt.
- **avkorting med restanse** (OMS beregning) er genuint komplekst – ikke forsøk å forenkle