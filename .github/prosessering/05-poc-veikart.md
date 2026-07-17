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

### Fase 4c — operatør-GUI ✅ GJORT (valg A: `etterlatte-saksbehandling-ui`, kun dev)

> **Blokker funnet ved deploy (2026-07-17):** `etterlatte-testdata` kjører i Azure-tenant
> **`nav.no`** (fordi den integrerer mot Dolly/testnav der), mens `etterlatte-behandling` og
> `etterlatte-saksbehandling-ui` kjører i **`trygdeetaten.no`**. En NAIS-app har nøyaktig én
> Azure-app i én tenant, og **OBO/client-credentials krysser ikke tenant-grenser** →
> `AADSTS500011: resource principal named api://dev-gcp.etterlatte.etterlatte-behandling was
> not found in the tenant`. testdata kan derfor **aldri** skaffe et token behandling godtar.
> Det forklarer også hvorfor dagens dolly-`BehandlingKlient` peker mot vedtaksvurdering *uten*
> token. Spec-antakelsen «testdata kaller behandling-REST med OBO» holder altså ikke.
>
> **Status:** behandling-siden (admin-API) er ferdig og deployet. testdata-frontenden er
> **parkert** (`ProsesseringFeature` er kommentert ut av `features`-lista i testdata
> `Application.kt` for å unngå live 500 i dev; klient/feature/template beholdes på disk).
>
> **Beslutning (2026-07-17): valg A — GUI i `etterlatte-saksbehandling-ui`.** Samme tenant
> (`trygdeetaten.no`), ekte operatør-flate, og BFF-en OBO-er allerede inn i behandling.
> **Kun dev** (både BFF-proxy og route/lenke er gatet), siden dette er PoC-innsyn og
> `payload` inneholder `fnrSoeker` (jf. V353 — bør maskeres ved evt. prod-tilpasning).
>
> **Gjennomført (valg A, 2026-07-17):**
> - **BFF-proxy** `/api/prosessering` → behandling (behandling-scope) i
>   `etterlatte-saksbehandling-ui/server`, **kun når `NAIS_CLUSTER_NAME !== 'prod-gcp'`**
>   (ny `erProduksjon` i `config.ts`). I prod finnes ruten ikke → 404.
> - **React-side** `ProsesseringTasks.tsx` (`client/src/components/prosessering/`) +
>   `prosesseringApi.ts`: lister tasks mot vår rene 5-status, statusfilter, «Rekjør»-knapp
>   for `STOPPET`/`AVBRUTT` (kaller `PUT …/rekjor`). Route `/prosessering` i `App.tsx` er
>   gatet bak `miljoeErDev`, og en dev-only lenke ligger i `AppSwitcher` («Utvikling (kun dev)»).
> - **NB:** admin-API-et er fortsatt gatet bak `ProsesseringToggles.PROSESSERING_ADMIN` i
>   behandling, så innsynet er dobbelt av (toggle av + kun-dev-proxy).
>
> **Åpent veivalg (historikk — valg A valgt):**
> - **A (anbefalt) — legg admin-siden i `etterlatte-saksbehandling-ui`.** Samme tenant, ekte
>   operatør-flate, BFF-en OBO-er allerede inn i behandling. Krever React/TS + BFF-proxy, dvs.
>   faktisk UI-design (som er bevisst utsatt).
> - **B — behold testdata-GUI, men gå via en trygdeetaten-app** (f.eks. gjøre
>   `etterlatte-testdata-behandler` til REST-bro). Awkward: den er en Kafka-river, og
>   testdata→den er også kryss-tenant.
> - **C — behold testdata-GUI kun lokalt/dev** der token-kravet kan omgås, ikke i sky.

Målet: kunne se og styre tasks (liste, detaljer, rekjøre) fra et GUI **uten** å bygge
et fullverdig operatør-UI ennå, og **uten** å conforme mot noe eksternt kontrakt-format.

**Beslutning (2026-07-17): bruk `etterlatte-testdata` sitt eget GUI, ikke
`familie-prosessering-frontend`.** Vi eier `etterlatte-testdata` fullt ut, den har allerede
et enkelt Mustache-GUI, og brukes allerede til å teste appene våre. Da slipper vi å bro vår
**5-status-modell** mot familie-prosesserings gamle 8-status + `Ressurs`-konvolutt +
avvikshåndtering/kommentar/logg. Vi eksponerer vår rene 5-status
(`KLAR`/`KJØRER`/`FULLFØRT`/`STOPPET`/`AVBRUTT`) direkte. **NB:** tenant-blokkeren over betyr
at *selve GUIet* trolig må flyttes fra testdata (retning A/B/C) — men admin-API-et under er
uansett riktig hjem og gjenbrukbart.

**Gjennomført (2026-07-17):**
1. **Lese-/rekjør-endepunkt i `etterlatte-behandling`** (`prosessering/ProsesseringAdminRoutes.kt`
   + `ProsesseringAdminDao.kt`) som returnerer vår 5-status direkte (ren `ProsesseringTaskDto`,
   ingen familie-former): `GET /api/prosessering/task` (filter `status`/`limit`),
   `GET /api/prosessering/task/{id}`, `PUT /api/prosessering/task/{id}/rekjor`. Saksbehandler-
   auth (`kunSaksbehandler`), gated bak ny toggle `ProsesseringToggles.PROSESSERING_ADMIN`
   (`"prosessering-admin"`, default av) — skilt fra `SKYGGE_SOEKNADMOTTAK` så innsyn og
   produksjon kan skrus av/på uavhengig. `ProsesseringAdminDao` (rå JDBC mot `prosessering.task`,
   samme mønster som `PostgresTaskRepository`) gjør `list`/`finn`/`rekjor`. `rekjor` er en manuell
   operatør-handling: `STOPPET`/`AVBRUTT` → `KLAR`, nullstiller `antall_feil` (=0) og
   `stoppaarsak` så motoren får fulle retries igjen (motorens `maxAntallFeil = 3`), og setter
   `trigger_tid = now()`. Wiret i behandlingens `Application.kt`. **Deployet til dev.**
2. **`ProsesseringFeature` i `etterlatte-testdata`** (`features/prosessering/`, PARKERT): Mustache-
   side (`templates/prosessering/tasks.hbs`) som lister tasks med statusfilter og «Rekjør»-knapp,
   `ProsesseringKlient` som skulle kalle behandling-admin-API-et med OBO. Blokkert av tenant-splitt
   (se over); kommentert ut av `features`-lista, filene beholdt for gjenbruk.
3. **Config/nais:** la til `behandling.app.url`/`behandling.app.scope` (env
   `ETTERLATTE_BEHANDLING_APP_URL`/`ETTERLATTE_BEHANDLING_APP_SCOPE`) i testdata — beholdt som
   parkert scaffolding, men fungerer ikke pga. tenant-splitt.

**Parkert alternativ (utforsket, ikke i bruk):** En adapter som eksponerer familie-
prosesserings `/api/task`-kontrakt (`Ressurs<T>`, 8-status, `taskStepType`, avvikshåndtering)
slik at `familie-prosessering-frontend` kunne gjenbrukes uendret, ble prototypet i en egen
økt og **lagret separat, men er ikke koblet inn på denne branchen**. Verdt å vite om vi
senere vil ha et ferdig fler-backend-GUI: da conformer vi mot familie-kontrakten (verifisert
mot frontendens `typer/task.ts`/`api/task.ts`), men for PoC-en er testdata-veien enklere og
holder oss i vår egen 5-status-modell.

### Fase 4d — Videre arbeid i Gjenny FØR eget repo (neste økter)
Vi blir værende i Gjenny og jobber videre med skyggekjøringen før Fase 5-uttrekket.
Det er flere ting å diskutere/undersøke her.

- **Rekjøring bevist ende-til-ende (2026-07-17). ✅ GJORT.**
  For å demonstrere den ene styrken rapids-and-rivers ikke gir alene — et **stoppet arbeid
  som kan plukkes opp igjen og fullføres** — la vi til en **feilbar demo-task** i host-en
  (`etterlatte-behandling/prosessering/FeilbarDemoTask.kt`). Den modellerer en **forbigående**
  feil: en simulert nedstrøms-avhengighet er «nede» frem til `simulertOppeFra`, så steget
  kaster (→ retry → STOPPET) og fullfører først når avhengigheten er «oppe».
  - **Task-type** `FeilbarDemo` med payload `FeilbarDemoPayload(demoId, simulertOppeFra)`.
    Ingen sideeffekter — steget bare kaster (nede) eller logger (oppe). Registrert i motoren
    via `installProsessering` (ved siden av skygge-steget).
  - **Produsent-endepunkt** `POST /api/prosessering/demo/feilbar` (`prosesseringDemoRoutes`)
    legger en demo-task i kø med `simulertOppeFra = now + vinduSekunder` (default 20 s).
    Saksbehandler-auth, gated bak `ProsesseringToggles.PROSESSERING_ADMIN` — samme operatør-flate
    som innsyns-/rekjør-API-et, og når via samme dev-only BFF-proxy `/api/prosessering`.
  - **GUI:** en «Opprett demo-task»-knapp i `ProsesseringTasks.tsx` gjør hele loopen klikkbar:
    opprett → se den gå STOPPET → vent til vinduet har gått → «Rekjør» → se den gå FULLFØRT.
  - **Bevist på Testcontainers** (`FeilbarDemoRekjorIntegrationTest`): task feiler → STOPPET
    (`Stoppaarsak.FEIL`), `ProsesseringAdminDao.rekjor` → KLAR, motoren plukker den → FULLFØRT.
    Steget observerer aldri en fullføring mens avhengigheten er «nede». Grønn.
  - **Merk:** dette er en *demo* av mekanikken (rekjør av STOPPET → FULLFØRT), ikke ekte arbeid.
    Skygge-tasken (`SoeknadMottakSkygge`) er fortsatt deterministisk — en rekjør av dens STOPPET
    ville feile likt igjen, siden validering er ren funksjon av payload. Demo-tasken finnes nettopp
    fordi rekjøring bare gir mening når feilen er forbigående.

- **Duplikate tasker? — undersøkt (2026-07-17). ✅ Avklart, tiltak gjenstår.**
  Dump av `prosessering.task` (33 rader) viste: alle `SoeknadMottakSkygge`, alle `FULLFØRT`,
  `antall_feil=0`, men kun **3 unike `soeknadId`** — `11764` (29 tasker, jevnt ~hvert 50. min),
  `11786` (2) og `11787` (2).
  - **Ikke motor-duplikater.** Hver rad har egen `id`/`opprettet_tid`/`trigger_tid`; ingen task
    kjørt to ganger (`execution_log.uq_execution_log_task` ville ellers slått ut), ingen
    retry-loop (`antall_feil=0`). Motoren håndterte hver task nøyaktig én gang — det er sunt.
  - **Produsent-duplikater.** `opprettFrittstående` (i `ProsesseringModule.kt`) gjør en ren
    `insert` — ingen unik-constraint på `(type, payload)` i `schema.sql`, ingen dedupe-sjekk.
    Skygge-riveren er dessuten bevisst mutasjonsfri (ingen `precondition`/`publish`), så
    `soeknad_innsendt` for samme søknad blir aldri «brukt opp» og redeleveres på hver
    rapid-syklus → ny task hver gang. Dette forklarer den jevne re-innkøingen av `11764`.
  - **Konklusjon: forventet gitt dagens design** — skyggekjøringen beviser
    reliable/retryable/observerbar håndtering. Men re-innkøingen bekrefter at **idempotens
    ikke er løst**, og det må på plass før skygge → ekte outbox.
  - **Anbefaling:** løs det **produsent-side** i skygge-ruten nå: sjekk om det allerede finnes
    en uferdig (`KLAR`/`KJØRER`) task for samme `soeknadId` før `opprettFrittstående`, og hopp
    over hvis så. Enkelt, holder biblioteket rent, og `soeknadId` er den naturlige
    idempotens-nøkkelen for denne task-typen. **Ikke** legg en generell `(type, payload)`-
    unik-constraint i biblioteket ennå — hva som utgjør «samme task» er verts-domene, ikke
    infra, og en bibliotek-side unik-nøkkel er en større API-beslutning som i så fall må
    besluttes og dokumenteres i `04-outbox-api.md` først.

### Fase 5 — Kutt ut til eget repo (etter Fase 4d)
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

- **Operatør-GUI (Fase 4c):** behandling-admin-API-et er ferdig og deployet.
  **Valg A implementert:** GUI-et ligger nå i `etterlatte-saksbehandling-ui` (React-side +
  dev-only BFF-proxy og lenke), som løser tenant-splitten (samme tenant, BFF-en OBO-er inn i
  behandling). `etterlatte-testdata`-GUIet forblir parkert. Familie-frontend-adapteren er
  fortsatt parkert.
- **NB personvern:** `ProsesseringTaskDto.payload` inneholder `fnrSoeker` (jf. V353). Vises i
  GUIet (kun dev) — bør maskeres/utelates ved en evt. prod-tilpasning.
- **Duplikate tasker? (Fase 4d) — ✅ undersøkt 2026-07-17.** Ikke motor-duplikater (hver task
  kjørt én gang, feil=0), men produsent-duplikater: samme `soeknadId` køes på nytt fordi
  `opprettFrittstående` ikke deduper og skygge-riveren ikke konsumerer eventet. Forventet for
  skyggen. **Anbefalt tiltak:** dedupe produsent-side på `soeknadId` (sjekk uferdig task før
  innkøing); *ikke* bibliotek-side unik-constraint uten en egen API-beslutning i
  `04-outbox-api.md`. Detaljer i Fase 4d over.
- **Bli i Gjenny litt til:** vi jobber videre med skyggekjøringen (Fase 4d) FØR Fase 5-uttrekket.
- Nøyaktig hvordan koble task-opprettelse på søknad-eventet uten å forstyrre dagens flyt
  (Fase 4 — via behandling-REST).
- Når går vi fra skygge til ekte outbox (task i samme tx som behandling)?
