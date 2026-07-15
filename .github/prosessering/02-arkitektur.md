# Greenfield prosessering — designplan

## Kontekst

Team Enslig Forsørger (folketrygdloven kap. 15) og Team Etterlatte (kap. 17) har
slått seg sammen til ett bredt domene, men kjører to ulike stacker: EF-apper
(familie-ef-sak m.fl.) er **Spring Boot**, Gjenny + apper er **Ktor /
rapids-and-rivers**. Begge er Kotlin.

Begge løpene ønsker funksjonaliteten som `familie-prosessering` gir EF/BAKS i dag: en
**pålitelig task queue med retry og operatørinnsyn** — batcharbeid som feiler tydelig,
kan inspiseres og kjøres på nytt fra et UI. Men dagens verktøy er en Spring Boot-library
(Java→Kotlin-port, gammel internlogikk) som Gjenny ikke kan ta i bruk som den er.

**Mål:** et Kotlin-verktøy for task-prosessering bygget fra scratch, uavhengig av
rammeverk, som *både* en Spring Boot-app og en Ktor-app kan bygge inn. Lettvekts, eid og
vedlikeholdbart — en bevisst masterclass, ikke en port.

### Hva prosessering egentlig er (den bærende innsikten)

Det er en **transactional-outbox job queue**: en task-rad legges inn *i samme
DB-transaksjon* som forretningsskrivingen, noe som garanterer at-least-once-kjøring. En
bakgrunns-poller claimer klare tasks, kjører matchende `TaskStep` og driver en
statusmaskin (`UBEHANDLET → PLUKKET → BEHANDLER → FERDIG`, med `FEILET` /
`MANUELL_OPPFØLGING` ved uttømming). Rundt dette: rekjøring, manuell oppfølging,
avvikshåndtering, kommentarer, logger per task.

Outbox-egenskapen avgjør **library vs app**: alt som berører vertens DB (produsent-API,
engine, persistens) **må** være en innebygd library — en selvstendig tjeneste ville
gjeninnført dual-write-problemet. Problemet med dagens verktøy er Spring-kobling + alder,
ikke arkitekturen. Vi beholder formen og bygger innmaten på nytt.

### Forholdet til rapids-and-rivers (Gjenny)

**Komplement, ikke erstatning.** Rapids = fire-and-broadcast-hendelseskoreografi
(eventual). Prosessering = pålitelig arbeid med retry og operatørinnsyn *innenfor* en
tjeneste, med outbox-garanti. De kan kombineres: en `TaskStep` kan publisere til rapiden;
en river kan opprette en task. Core forblir transportagnostisk; en valgfri
`prosessering-rapids`-bridge kan komme senere.

## Låste beslutninger

| Beslutning | Valg |
|---|---|
| Engine-fundament | **Bygg en fokusert core** — egen coroutine engine; lån db-scheduler sin `SELECT … FOR UPDATE SKIP LOCKED`-teknikk uten avhengigheten |
| Skjema / kontrakt | **Rent brudd** — design ideelt skjema + REST-kontrakt, lever en engangsmigrering for EF |
| Sekvensering | **Begge adaptere (Spring + Ktor) fra dag én**, håndhevet av et felles kontraktstest-kit |

## Arkitektur — mono-repo, Gradle multi-module

```
prosessering/                       (one repo)
├── prosessering-core/              pure Kotlin, zero framework
├── prosessering-postgres/          default repository (JDBC + Flyway + SKIP LOCKED)
├── prosessering-api/               shared REST DTOs + OpenAPI contract
├── prosessering-tck/               behavioral contract test kit (both adapters must pass)
├── prosessering-spring-boot-starter/   autoconfig + @RestControllers + Spring Security
├── prosessering-ktor/              install(Prosessering) plugin + routes + nav-token-support
└── prosessering-rapids/            (optional, later) rapids-and-rivers bridge
```

Bruk `buildSrc` / convention plugins (`kotlin-library`, `spring-starter`) for konsekvent
moduloppsett og én versjonskatalog (`libs.versions.toml`).

### `prosessering-core` (masterclassen)

Ren Kotlin. Inneholder domenet, engine og **ports** (grensesnitt) — ingen DB, ingen web.

- **Domene:** `Task`, `Status`, `TaskLogg`, `Avvikstype`, `TaskStep<T>` interface,
  `@TaskStepBeskrivelse(type, beskrivelse, maxAntallFeil, triggerTidVedFeilISekunder,
  settTilManuellOppfølgning)`, `RekjørSenereException`, `TaskExceptionUtenStackTrace`.
- **Typede payloads** — `TaskStep<T>` med en payload-(de)serializer, som erstatter dagens
  rå `String`-payload + utypede `Properties`. Metadata (callId/fagsakId/behandlingId) er
  et separat typet map som propageres via MDC.
- **Eksplisitt `TaskStateMachine`** — én sannhetskilde for lovlige overganger. (I dag er
  disse spredt på TaskWorker/TaskService/TaskScheduler — en sentral tech debt-fiks.)
- **Coroutine engine** — superviset polling-loop:
  `while (isActive) { claimBatch(); launch each on bounded dispatcher; delay(interval) }`,
  structured concurrency for kontrollert nedstenging, en `Semaphore` for backpressure. Erstatter
  Spring `@Scheduled` + `ThreadPoolTaskExecutor`.
- **Ports (vertapp / adaptere implementerer):** `TaskRepository`, `LeaderElection`,
  `CurrentUser`, `AccessCheck`, `Metrics`, `Clock`, `SecureLog`. Dette splitter dagens
  `ProsesseringInfoProvider` god-interface i fokuserte deler.

### `prosessering-postgres`

Standard `TaskRepository` på plain JDBC (HikariCP) + Flyway. **Ingen Spring Data** — samme
SQL betjener begge adaptere. Det er her db-scheduler sitt samtidighetstriks lånes:

Atomisk claim (ingen dobbelkjøring på tvers av pods, ingen retry-dans med optimistic lock):
```sql
WITH klar AS (
  SELECT id FROM task
   WHERE status IN ('UBEHANDLET','KLAR_TIL_PLUKK') AND trigger_tid <= now()
   ORDER BY trigger_tid LIMIT :batch
   FOR UPDATE SKIP LOCKED
)
UPDATE task SET status='PLUKKET', plukket_tid=now(), versjon=versjon+1
 WHERE id IN (SELECT id FROM klar) RETURNING *;
```

**Ideelt skjema (rent brudd):**

`task`
- `id BIGINT GENERATED ALWAYS AS IDENTITY` (billig, index-lokal; UUIDv7 nevnt som alternativ)
- `type VARCHAR`, `payload TEXT` (JSONB valgfritt for UI-søk; payload er sensitiv)
- `status VARCHAR(20)`, `avvikstype VARCHAR NULL`
- `trigger_tid / opprettet_tid / sist_kjort / plukket_tid TIMESTAMPTZ`
- `antall_feil INT` (eksplisitt retry-teller, ikke utledet fra logger)
- `metadata JSONB` (callId, fagsakId, behandlingId, …)
- `versjon BIGINT` (belt-and-suspenders concurrency / API ETag)
- Delvis hot-path-indeks:
  `CREATE INDEX idx_task_plukk ON task (trigger_tid)
     WHERE status IN ('UBEHANDLET','KLAR_TIL_PLUKK');`

`task_logg` (samlet eventlogg — slår sammen dagens dupliserte `Loggtype`/`Status`)
- `id, task_id (FK), hendelse (STATUS_ENDRET|KOMMENTAR|AVVIK), ny_status VARCHAR NULL,
   endret_av, node, melding TEXT NULL, opprettet_tid` + indeks på `task_id`.

**Vedlikehold (leader-styrt):** daglig retry av `FEILET`, slett `FERDIG` eldre enn N
uker, metrikk-tellinger og en **reaper** som resetter `PLUKKET`/`BEHANDLER` eldre enn en
timeout (dekker at en pod dør midt i en task — `plukket_tid` styrer dette). **Standard for
leader election = Postgres advisory lock** (`pg_try_advisory_lock`), rammeverksagnostisk
og trenger ingen ekstern elector; NAV elector forblir et pluggbart alternativ.

### Adaptere (begge fra dag én)

- **`prosessering-spring-boot-starter`** — `@AutoConfiguration` som wirer core+postgres,
  engine-livssyklus via `SmartLifecycle`, `@RestController`s for API-et, Spring Security +
  `nav-token-support` for auth, Micrometer `Metrics`. Drop-in for **familie-ef-sak**.
- **`prosessering-ktor`** — `val Prosessering = createApplicationPlugin{…}`;
  engine-livssyklus via `ApplicationStarted`/`ApplicationStopped`, samme ruter,
  `nav-token-support` auth, Micrometer. Drop-in for **Gjenny**.

### `prosessering-api` — én REST-kontrakt (OpenAPI-first)

```
GET  /api/task?status=&type=&page=        list (paginated)
GET  /api/task/{id}                       detail (payload + metadata)
GET  /api/task/{id}/logg                  task history
GET  /api/task/callId/{callId}            full processing chain
GET  /api/task/types                      known task types
GET  /api/task/count                      counts per status + antall-til-oppfølging
POST /api/task/{id}/rekjor                rerun one
POST /api/task/rekjor?status=&type=       bulk rerun
POST /api/task/{id}/avvik                 {avvikstype, årsak}
POST /api/task/{id}/kommentar             {settTilManuellOppfølging, kommentar}
```
Enkle REST-ressurser + RFC 7807 `application/problem+json` for feil (ryddigere enn den
gamle `Ressurs<T>`-innpakningen; innpakning nevnes som det kjente alternativet). OpenAPI-specen
er den ene sannhetskilden for API-et.

### `prosessering-tck` — kontraktstest-kit (kritisk for "begge adaptere")

En gjenbrukbar atferdssuite som både postgres-repoet og *hver adapter* må passere:
claim/ingen dobbelkjøring, retry/backoff, ruting til manuell oppfølging, reaper,
statusmaskin-lovlighet, REST-kontrakt-paritet. Det er dette som garanterer at Spring og
Ktor oppfører seg identisk.

### Sikkerhet

- Authz i adapterlaget via `AccessCheck`-porten (AD-gruppestyring; vertappen eier
  regelen). Azure AD / Entra for operatører.
- **Sensitive payloads** (personnummer, kap. 15/17-data): behold secure-log-splitten;
  audit-logg operatørhandlinger (hvem kjørte på nytt / så hvilken task); styr visning av
  sensitiv payload med tilgangslogging; oppbevaring via autosletting av ferdige tasks.

### Navn — `efterlatte-prosessering` (besluttet)

**Efterlatte** = et teleskopord av **EF** (Enslig Forsørger) + **Etterlatte** — den nye
kombinerte teamidentiteten, som også nesten leses som et ekte ord (jf. dansk
*efterladte*). Repo / artefakter: `efterlatte-prosessering`.

## Byggesekvens

1. `prosessering-core` (domene, statusmaskin, coroutine engine, ports) **+** `prosessering-tck` mot et in-memory repo.
2. `prosessering-postgres` (JDBC-repo, Flyway, SKIP LOCKED, reaper, advisory-lock leader) — må passere TCK (Testcontainers).
3. `prosessering-api` (DTO-er + OpenAPI).
4. Parallelt: `prosessering-spring-boot-starter` og `prosessering-ktor` — hver passerer kontraktstester på adapter-nivå.
5. Integrer i én Spring-app (EF) og én Ktor-app (Gjenny).
6. EF-migrering: engangsskript gammelt skjema → nytt skjema; overgang.

## Verifisering

- **Statusmaskin:** unit/property-tester på lovlige overganger.
- **TCK:** kjører mot in-memory + Postgres (Testcontainers) + hver adapter.
- **Samtidighet:** N coroutines/pods claimer fra én Postgres → verifiser ingen dobbelkjøring.
- **Feilstier:** retry-teller, backoff `trigger_tid`, `MANUELL_OPPFØLGING` vs `FEILET`,
  ny planlegging for `RekjørSenereException`, reaper (kill mid-task → plukket opp igjen).
- **Adapter-paritet:** start en Spring-app og en Ktor-app, treff den identiske
  REST-kontrakten, verifiser lik oppførsel.
- **Migrering:** last et fixture med gammelt skjema, kjør migreringen, verifiser dataintegritet +
  at eksisterende tasks prosesseres under ny engine.
