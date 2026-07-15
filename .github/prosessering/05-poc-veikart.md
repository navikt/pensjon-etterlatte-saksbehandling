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
| `ktor` | ✅ (minimal) | `install(Prosessering)` — lifecycle + produsent-wiring. **Ingen REST-ruter ennå** |
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
- Typed `TaskStep<P>` / `TaskType<P>` + payload-(de)serialisering + produsent-API
  (`opprett` / `opprettFrittstående`, se [04-outbox-api.md](04-outbox-api.md)).
- Reaper: reset `KJØRER` eldre enn timeout → `KLAR` (dekker pod som dør midt i et steg).
- Flyway i stedet for rå `schema.sql`; flere ports ved behov (`Klokke`, `Metrics`).
- Behold konkurransebeviset.

### Fase 2 — `ktor`-plugin (minimal)
- `install(Prosessering)` som starter/stopper engine på `ApplicationStarted` /
  `ApplicationStopped`, og eksponerer `TaskProdusent`.
- Wires mot en Postgres i en Gjenny-Ktor-app.

### Fase 3 — `SoeknadMottakSkygge`-task-type
- Payload: `soeknadId`, `sakType` (BP/OMS), `fnrSoeker`.
- Steg: valider fnr + skjema, logg «ville opprettet behandling …». Ingen sideeffekter.

### Fase 4 — Skyggekjøring ende-til-ende i Gjenny
- Opprett task fra søknad-eventet (parallelt med `NySoeknadRiver`).
- Kjør i dev; observer at søknader blir tasks som fullfører.
- Fremtving feil → STOPPET → prøv igjen (via kall/logg siden UI er utenfor scope).

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

## Åpne tråder å sparre om videre

- Hvor skal task-tabellene bo i Gjenny? (Egen app-DB, f.eks. `etterlatte-behandling`,
  eller et dedikert lite skjema.) For skygge holder `opprettFrittstående`.
- Nøyaktig hvordan koble task-opprettelse på søknad-eventet uten å forstyrre dagens flyt.
- Når går vi fra skygge til ekte outbox (task i samme tx som behandling)?
