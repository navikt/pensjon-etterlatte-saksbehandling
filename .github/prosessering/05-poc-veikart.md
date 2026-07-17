# PoC-veikart — skyggekjøring av søknadsmottak i Gjenny

> Aktivt arbeidsdokument. Companion til [01-intensjon.md](01-intensjon.md) (hvorfor),
> [02-arkitektur.md](02-arkitektur.md) (arkitektur) og [04-outbox-api.md](04-outbox-api.md)
> (produsent-API). Dette dokumentet definerer det **avgrensede PoC-scopet** og veien dit.

---

## Målet med PoC-en

Bevise at prosessering-konseptet gir verdi **inne i Gjenny (Ktor)** ved å la Gjenny ta
imot søknader på en prosesserings-måte — en **skyggekjøring**:

- En innsendt søknad fører til at det opprettes en **prosessering-task**.
- Motoren plukker og kjører en `TaskStep` som **simulerer** mottaket (validerer payload,
  logger «ville opprettet behandling for sak X av type Y») — **uten** å faktisk kalle
  `etterlatte-behandling` eller opprette sak/behandling.
- Når steget «feiler» (kan fremtvinges), havner tasken i **STOPPET** og kan **prøves
  igjen**.

Dette demonstrerer det rapids-and-rivers *ikke* gir alene: reliable, retryable og
**operatør-observerbar** behandling med eksplisitt tilstand og manuell oppfølging.

**Ikke-mål for PoC:** faktisk opprette behandling, operatør-UI, REST-kontrakt,
Spring-adapter, TCK, ef-sak-migrering, ekte outbox-kobling til et forretnings-skriv.

---

## Slik tas søknader imot i Gjenny i dag (utgangspunktet)

```
innsendt-soeknad ──(Kafka: etterlatte.etterlatteytelser)──▶ NySoeknadRiver /
                                                            OpprettBehandlingRiver
                                                            (etterlatte-behandling-kafka)
                                                                    │
                                                                    ▼
                                                          oppretter sak + behandling
```

Event: `soeknad_innsendt` (`SoeknadInnsendtHendelseType.EVENT_NAME_INNSENDT`).
Nøkkelfelter: `@skjema_info`, `@lagret_soeknad_id`, `@template`, `@fnr_soeker`,
`@hendelse_gyldig_til`. Skjemamodell: `InnsendtSoeknad` (fra `pensjon-etterlatte-felles/common`).

Skyggekjøringen kjører **parallelt** med dette — den erstatter ingenting, den observerer.

---

## Skyggekjøringens flyt (PoC)

```
søknad-event ─▶ opprett SoeknadMottakSkygge-task ─▶ engine plukker (SKIP LOCKED)
                (opprettFrittstående*)                     │
                                                           ▼
                                              TaskStep: valider + logg «ville opprettet
                                              behandling (sakType, fnr, soeknadId)»
                                                           │
                                        ┌──────────────────┴──────────────────┐
                                        ▼                                      ▼
                                    FULLFØRT                    (fremtvunget feil) → STOPPET
                                                                    → prøv igjen
```

\* En ren skygge har ikke noe forretnings-skriv å henge outbox-garantien på, så vi bruker
`opprettFrittstående` (se [04-outbox-api.md](04-outbox-api.md)). PoC-en beviser **motor +
observerbarhet**; den ekte outbox-koblingen (task opprettet i samme tx som behandling)
kommer når vi går fra skygge til virkelig.

---

## Avgrenset modul-scope for PoC-en

Fra målbildets 7 moduler ([02-arkitektur.md](02-arkitektur.md)) skjærer vi til tre:

| Modul | I PoC? | Innhold |
|---|:---:|---|
| `core` | ✅ | Domene (5-status), state machine, coroutine-engine, ports, `TaskStep<P>` / `TaskType<P>`, produsent |
| `postgres` | ✅ | JDBC-repo, Flyway, `SKIP LOCKED`, reaper |
| `ktor` | ✅ (minimal) | `install(Prosessering)` — lifecycle + produsent-wiring **implementert**. Ingen REST-ruter ennå |
| `api` (REST/OpenAPI) | ❌ | Senere (trengs først med UI) |
| `tck` | ❌ | Senere |
| `spring-boot-starter` | ❌ | Senere (ef-sak) |

---

## Faser

### Fase 0 — Rydd PoC-koden ✅ GJORT
Besluttet: **start `core` rent** i stedet for å refaktorere den døde 8-status-koden.
Beholdt de to beviste ressursene (`SKIP LOCKED`-claim + `execution_log`-UNIQUE-bevis,
Testcontainers-harnesset). Koden bor nå som to biblioteksmoduler **inne i Gjenny**
(`pensjon-etterlatte-saksbehandling`, prototypes her, kuttes ut til eget repo ved Fase 5):
- **`libs/etterlatte-prosessering-core`** (ren Kotlin, ingen `java.sql`): 5-status
  `Status`, `Stoppaarsak`, `Task`, eksplisitt `TaskStateMachine` (én kilde til sannhet for
  lovlige overganger), `TaskRepository`-**port**, fikset `ProcessingEngine` (feilstier →
  STOPPET/FEIL og KLAR-retry med backoff).
- **`libs/etterlatte-prosessering-postgres`**: `PostgresTaskRepository` (JDBC + `SKIP
  LOCKED`), `schema.sql`.
- Integrert i Gjennys Gradle-build (versjonskatalog, `libs:`-moduler). Tester grønne:
  state machine (core) + konkurranse (1000 tasks, 4 engines, 0 dobbeltkjøring) + feil/retry
  (STOPPET + KLAR-retry) på Testcontainers.

### Fase 1 — Fullfør motoren
- ✅ **Typed `TaskStep<P>` / `TaskType<P>` + payload-serialisering.** `TaskType<P>`
  bærer `serialiser`/`deserialiser` som funksjoner → `core` binder seg ikke til
  Jackson/kotlinx og forblir framework-agnostisk. `strengType(navn)` for rå-streng-payload.
  `TaskStep<P>` er **blokkerende** (ikke `suspend`) — kjøres i én tx på én tråd.
- ✅ **Produsent-API `opprett` / `opprettFrittstående`** (se [04-outbox-api.md](04-outbox-api.md)).
  `Transaksjon`-markørinterface + `TaskId` i `core`; `JdbcTransaksjon(connection)` i
  `postgres`. `StandardTaskProdusent` serialiserer og delegerer til repoet. `opprett`
  skriver på kallerens tx; `opprettFrittstående` åpner sin egen. Outbox-garantien
  bevist i `OutboxTest` (rollback tar tasken med seg; commit beholder den).
- ✅ **Transaksjon-per-steg** (beslutning 2): `TaskRepository.iEgenTransaksjon { tx -> … }`
  pakker hver stegkjøring; `markerFullført(tx, id)` committer atomisk med stegets skriv.
  `TaskKontekst.opprettNesteTask(...)` legger neste task på samme tx (atomiske kjeder).
  Feil → rollback + feil-bokføring i egen tx (KLAR m/backoff eller STOPPET).
- ✅ **Reaper**: `KJØRER` eldre enn `plukket_tid`-timeout → `KLAR` (dekker pod som dør
  midt i et steg). `Reaper` i `core` + `TaskRepository.gjenopprettHengende(plukketFoer)`.
  Teller *ikke* som retry (`antall_feil` røres ikke). Testet på Testcontainers.
- **Skjema-støtte:** `PostgresTaskRepository(skjema = "prosessering")` — SQL-en er
  skjema-kvalifisert, default `prosessering`. Bibliotekets `schema.sql` (test) oppretter
  skjemaet. Klart for behandling-DB (se «Besluttet: hvor task-tabellen bor»).
- **Gjenstår:** flere ports ved behov (`Klokke`, `Metrics`). *(Flyway-migrasjonen er nå lagt
  inn som `V352` i behandling — se Fase 4a.)*
- ✅ Konkurransebeviset beholdt (1000 tasks, 4 engines, 0 dobbeltkjøring) — nå via
  typed steg + produsent.

### Fase 2 — `ktor`-plugin (minimal) ✅ GJORT
- **`libs/etterlatte-prosessering-ktor`**: `install(Prosessering)` bygget som en
  `createApplicationPlugin`. Konfig (`ProsesseringConfig`) tar vertens `repository`
  (adapter mot dens DB), `steg` og `node`; motor/reaper beholder egne standarder.
- **Livssyklus wiret:** `ApplicationStarted` → `engine.start()` (+ reaper), og
  `ApplicationStopPreparing` → `engine.stop()`/`reaper.stop()` i `runBlocking`
  (graceful shutdown). Ingen REST-ruter, som planlagt.
- **Produsent eksponert** via `Application.taskProdusent` (attribute-nøkkel), så
  ruter/handlere kan legge arbeid i kø etter `install`.
- Ende-til-ende-test på Testcontainers: appen starter → motoren plukker en KLAR
  `opprettFrittstående`-task og kjører den til `FULLFØRT`. Grønn.
- **Reaperen kan slås av** (`reaperPaa = false`) for test/kontekster uten behov.

### Fase 3 — `SoeknadMottakSkygge`-task-type ✅ GJORT
- **Typed payload** `SoeknadMottakPayload(soeknadId, sakType, fnrSoeker)` med `SakType`
  (BP/OMS). Task-typen bærer sin egen Jackson-(de)serialisering → `core` forblir
  framework-agnostisk; verten velger serialiseringsteknologi.
- **Steg** validerer fnr (11 siffer) + skjema (soeknadId ikke tom) og *logger* «ville
  opprettet behandling …» — **ingen sideeffekter**, ingen kall til `etterlatte-behandling`.
  Validering som feiler kaster → motoren gir KLAR-retry og til slutt STOPPET (FEIL).
- **Plassert i ktor-modulens testkilde** (`…ktor.skygge`) med vilje: dette er *vertens*
  domene, ikke gjenbrukbar prosessering-infra, så det skal ikke ligge i biblioteks-main.
  Ved Fase 4 flyttes tilsvarende task-type inn i `etterlatte-behandling` (host-en).
- **Bevist på Testcontainers:** gyldig søknad → task → FULLFØRT (mottaket observert), og
  ugyldig fnr → retry → STOPPET (Stoppaarsak.FEIL), uten at steget noensinne observerer
  en ugyldig søknad.

### Fase 4 — Skyggekjøring ende-til-ende i Gjenny
**4a — host-kobling (ugatet mot prod, kjørt/verifisert på Testcontainers) ✅ GJORT**
- **Task-typen flyttet inn i `etterlatte-behandling`** (`prosessering/SoeknadMottakSkygge.kt`):
  `SoeknadMottakPayload` bruker vertens ekte `SakType` og `objectMapper`; steget validerer
  (`Folkeregisteridentifikator.isValid` + soeknadId) og *logger* «ville opprettet behandling …»
  — ingen sideeffekter. Fase 3-koden i ktor-lib-testkilden er fjernet (ekte «flytt»).
- **Motoren wiret via `install(Prosessering)`** på `Route.application`, men **kun i
  produksjons-oppsettet** (`installProsessering` kalles fra `initEmbeddedServer(routes = …)`),
  *ikke* fra den delte `module`-testinngangen — så motoren/reaperen ikke starter i den store
  mengden behandling-tester. Motoren poller `prosessering.task` og er inaktiv uten tasker.
- **Internt systembruker-endepunkt** `POST /api/prosessering/skygge/soeknad`
  (`prosesseringSkyggeRoutes`, inne i `authenticate`) legger en skygge-task i kø via
  `opprettFrittstående`. Gated bak `ProsesseringToggles.SKYGGE_SOEKNADMOTTAK`
  (`"prosessering-soeknad-skygge"`, default av).
- **`SoeknadSkyggeRiver`** i `etterlatte-behandling-kafka`, parallelt med `NySoeknadRiver`
  (samme `soeknad_innsendt`-event, **ingen** `precondition`/`publish` som konsumerer eventet).
  Kaller endepunktet via `BehandlingClient.opprettSoeknadSkyggeTask`. Også gated bak samme
  toggle-nøkkel (i egen enum i kafka-appen), så ingen HTTP-kall når mørkt.
- **Flyway `V352__prosessering_skjema.sql`** i behandling oppretter `prosessering`-skjemaet +
  `task`-tabellen + plukk-indeksen (ikke `execution_log` — den er test-only). Kjører i dev nå;
  prod-tilpasning ved behov senere.
- **Integrasjonstest** (`SoeknadMottakSkyggeIntegrationTest`, Testcontainers via behandlingens
  `GenerellDatabaseExtension`): gyldig søknad → task → FULLFØRT (mottaket observert), og
  ugyldig fnr → retry → STOPPET (`Stoppaarsak.FEIL`) uten at mottaket noensinne observeres. Grønn.

**4b — kjør i dev (gjenstår, krever menneske-i-loop):**
- Deploy behandling (med V352) + behandling-kafka til dev.
- **Opprett feature-flagget `prosessering-soeknad-skygge` i team-Unleash (Unleash-web-UI-et,
  ikke nais-yaml) og aktiver det for dev.** Ett flagg styrer begge appene siden de deler
  nøkkelen. Til flagget finnes returnerer SDK-en default `false` → skyggekjøringen er mørk
  (trygt av seg selv).
- Flipp flagget på i dev; observer at søknader blir tasks som fullfører.
- Fremtving feil → STOPPET → prøv igjen (via kall/logg siden UI er utenfor scope).

### Fase 4c — operatør-GUI via `etterlatte-testdata` (VALGT RETNING — neste økt)

Målet: kunne se og styre tasks (liste, detaljer, rekjøre) fra et GUI **uten** å bygge
et fullverdig operatør-UI ennå, og **uten** å conforme mot noe eksternt kontrakt-format.

**Beslutning (2026-07-17): bruk `etterlatte-testdata` sitt eget GUI, ikke
`familie-prosessering-frontend`.** Vi eier `etterlatte-testdata` fullt ut, den har allerede
et enkelt Handlebars-GUI, kaller behandling-REST med saksbehandler-token (OBO), og brukes
allerede til å teste appene våre. Da slipper vi å bro vår **5-status-modell** mot familie-
prosesserings gamle 8-status + `Ressurs`-konvolutt + avvikshåndtering/kommentar/logg. Vi kan
eksponere vår rene 5-status (`KLAR`/`KJØRER`/`FULLFØRT`/`STOPPET`/`AVBRUTT`) direkte.

**Hvorfor dette er lettere (funn 2026-07-17):**
- `etterlatte-testdata` er en Ktor + Handlebars-app. Features implementerer `TestDataFeature`
  (`beskrivelse`, `path`, `kunEtterlatte`, `routes`) — se `EgendefinertMeldingFeature`.
- Den har allerede en `BehandlingKlient` som kaller behandling-REST via OBO-token
  (`features/dolly/BehandlingKlient.kt`). Samme mønster kan hente/rekjøre tasks.
- Ingen kontrakt å conforme til: GUIet er vårt, så list-/detalj-/rekjør-sidene tegnes rett
  mot vår egen modell. Ingen 5→8-status-oversettelse, ingen `taskStepType`/`Ressurs`-former.

**Skisse til gjennomføring (ikke startet):**
1. **Lese-/rekjør-endepunkt i `etterlatte-behandling`** som returnerer vår 5-status direkte
   (ren DTO, ingen familie-former): `GET /api/prosessering/task` (liste m/filter),
   `GET /api/prosessering/task/{id}`, `PUT /api/prosessering/task/{id}/rekjor`. Saksbehandler-
   auth (`kunSaksbehandler`), gated bak en toggle. En enkel read/admin-DAO mot
   `prosessering.task` (list, finn, rekjor: STOPPET/AVBRUTT → KLAR).
2. **`ProsesseringFeature` i `etterlatte-testdata`** (`TestDataFeature`): Handlebars-side som
   lister tasks (status, type, antall_feil, trigger_tid, stoppårsak) og har en «rekjør»-knapp,
   via en `ProsesseringKlient` (kopi av `BehandlingKlient`-mønsteret, OBO mot behandling).
3. **Bruk eksisterende søknad-/egendefinert-melding-features** i testdata til å *trigge*
   skygge-tasks (via `soeknad_innsendt`-flyten fra 4a/4b), og den nye siden til å *observere
   og styre* dem. NB fra 4b: `etterlatte-testdata` publiserer i dag `trenger_behandling`, ikke
   `soeknad_innsendt` — for å trigge `SoeknadSkyggeRiver` må vi enten poste `soeknad_innsendt`
   (f.eks. via egendefinert-melding-featuren) eller sende ekte søknad via søknadsdialogen.

**Parkert alternativ (utforsket, ikke i bruk):** En adapter som eksponerer familie-
prosesserings `/api/task`-kontrakt (`Ressurs<T>`, 8-status, `taskStepType`, avvikshåndtering)
slik at `familie-prosessering-frontend` kunne gjenbrukes uendret, ble prototypet i en egen
økt og **lagret separat, men er ikke koblet inn på denne branchen**. Verdt å vite om vi
senere vil ha et ferdig fler-backend-GUI: da conformer vi mot familie-kontrakten (verifisert
mot frontendens `typer/task.ts`/`api/task.ts`), men for PoC-en er testdata-veien enklere og
holder oss i vår egen 5-status-modell.

### Fase 5 — Kutt ut til eget repo
- Når form/API sitter: opprett `efterlatte-prosessering`-repoet, publiser som
  Maven-artefakt (GitHub Packages, à la `pensjon-etterlatte-felles/common`), la Gjenny
  dra det inn via Gradle-avhengighet i stedet for `includeBuild`.

---

## Hvor bygger vi (besluttet + gjort)

**Prototypes inne i Gjenny** (`pensjon-etterlatte-saksbehandling`) som to
biblioteksmoduler i `libs/`, integrert i Gjennys Gradle-build (versjonskatalog). Rask
iterasjon og direkte integrasjonspunkt mot ekte DB og søknad-events. Kuttes ut til eget
`efterlatte-prosessering`-repo ved Fase 5 når API-et er stabilt.

**Vaktpost:** `core` skal aldri importere Gjenny-typer (`java.sql`/Ktor/behandling) —
håndhevet av modulskillet, slik at uttrekket senere blir et rent kopier-ut.

---

## Besluttet: hvor task-tabellen bor

**Task-tabellen bor i `etterlatte-behandling` sin database, i et eget Postgres-skjema
`prosessering`.** Hard føring fra outbox-beslutningen: tasken må skrives i samme
DB/transaksjon som behandlings-skrivet (ellers dual-write), og behandlingen opprettes
i behandling-REST. Sentraliseringen mot behandling peker samme vei. Motoren embeddes i
behandling-REST (Ktor) via `install(Prosessering)`.

- **Eget skjema `prosessering`** (ikke behandlings hovedskjema): logisk isolasjon fra de
  ~237 behandlings-migrasjonene, ingen navnekollisjon på generiske navn (`task`), og et
  rent kutt-ut ved Fase 5. Outbox holder — samme connection skriver på tvers av skjema i
  samme database. `PostgresTaskRepository(skjema = "prosessering")` er default; SQL-en er
  skjema-kvalifisert.
- **Skygge koblet på søknad-eventet (Fase 4a)** via `SoeknadSkyggeRiver` (parallell river) →
  behandling-REST → `opprettFrittstående`. Bevisst *skygge*-kobling, ikke ekte outbox: målet er
  å bevise motor + observerbarhet. Den ekte outbox-koblingen (task i samme tx som behandlings-
  skrivet) hører hjemme i skygge→ekte-overgangen, ikke her — den ville vært mer invasiv på
  hot-pathen, og outbox-garantien er allerede bevist i `OutboxTest`.
- **Nyanse:** søknad-eventet konsumeres i `etterlatte-behandling-kafka` (egen app), mens
  produsent + motor hører hjemme i behandling-REST (regel: ingen app skriver i en annen
  apps DB). Event-drevet task-opprettelse må derfor gå *via* behandling-REST når den tid
  kommer.

**Lagt inn (Fase 4a):** Flyway `V352__prosessering_skjema.sql` oppretter `prosessering`-skjemaet
+ `task`-tabellen i behandling-DB. Kjører i dev nå; prod-tilpasning tas ved behov senere
(besluttet at dev holder for PoC-en).

**Grants-lærdom (V353):** V352 opprettet skjemaet uten grants. På `POSTGRES_14` lener
`public`-skjemaet seg på PostgreSQLs default PUBLIC-privilegier, men et *nytt* skjema arver
ikke det — kun eieren (Flyway/app-brukeren) får tilgang. Motoren kjører derfor fint, men
lesende roller (personlige IAM-brukere) får «permission denied for schema prosessering».
`V353__prosessering_skjema_grants.sql` replikerer public-mønsteret (`GRANT USAGE` + `SELECT`
til `PUBLIC` + `ALTER DEFAULT PRIVILEGES`). NB: `task.payload` inneholder `fnrSoeker` — greit
i dev/PoC og konsistent med at hele public-skjemaet allerede eksponeres til PUBLIC på PG14,
men strammes inn ved en evt. prod-tilpasning.

---

## Åpne tråder å sparre om videre

- **Operatør-GUI (Fase 4c):** valgt retning er `etterlatte-testdata` sitt eget GUI (se
  Fase 4c). Gjenstår: lese-/rekjør-endepunkt i behandling + `ProsesseringFeature` i testdata.
  Familie-frontend-adapteren er parkert.
- Nøyaktig hvordan koble task-opprettelse på søknad-eventet uten å forstyrre dagens flyt
  (Fase 4 — via behandling-REST).
- Når går vi fra skygge til ekte outbox (task i samme tx som behandling)?
