# Outbox produsent-API — designbeslutning (låst 2026-07-02)

> Følgedokument til `02-arkitektur.md` (arkitektur). Dette låser det ene mest
> bærende designet i core-biblioteket: **hvordan en task opprettes inne i
> vertsapplikasjonens transaction**, framework-agnostisk.

## Problemet

"Outbox" betyr at `INSERT INTO task` går på **samme JDBC-connection, inne i samme
transaction** som forretnings-skrivet — enten committer begge, eller ingen av dem.
De to vertene håndterer transactions på motsatte måter:

- **familie-ef-sak (Spring)** — transactions er *ambient*: `@Transactional` binder en
  connection til tråden; ingen sender connections rundt.
- **Gjenny (Ktor)** — transactions er *eksplisitte*: en connection er en verdi du holder
  på og sender videre.

Core kan ikke være avhengig av noen av idiomene. Spørsmålet er: hva tar `opprett(...)`
imot som transaction-argument, og hvem leverer det?

## Låste beslutninger

| # | Beslutning | Valg |
|---|---|---|
| 1 | Transaction-søm i core | **Markørinterface `Transaksjon`** eies av core; `JdbcTransaksjon(connection)` ligger i postgres-modulen. Core importerer aldri `java.sql`; in-memory TCK-repoet får en naturlig erstatning; ikke-JDBC-lagring er fortsatt mulig. |
| 2 | Kjøring på engine-siden | **Transaction per step.** Engine pakker hver step-kjøring inn i sin egen transaction (`TaskKontekst` bærer den); statusoppdateringen til `FULLFØRT` committer atomisk med stepets DB-skriv. |
| 3 | Tasks uten forretnings-tx | **Eksplisitt `opprettFrittstående(...)`** som åpner sin egen lille transaction. Det tydelige navnet signaliserer at "outbox er bevisst ikke i spill". Standard `opprett` starter aldri en egen transaction. |

**Forkastet:** ambient/ThreadLocal transactions i core. Feilmodusen — å stille åpne en
egen transaction når ingen er bundet — er akkurat den dual-write-feilen dette biblioteket
finnes for å eliminere. Det fungerer også dårlig med coroutines (ThreadLocal følger ikke
suspension) og Gjennys eksplisitte stil.

## Lagdelingen

```
core        TaskProdusent.opprett(transaksjon, type, payload)   ← the guarantee lives here
postgres    JdbcTransaksjon(connection) : Transaksjon           ← the JDBC binding
spring      TaskService.opprett(type, payload)                  ← finds Spring's ambient tx, fails loudly without one
ktor        pass-through — Gjenny already holds its connection  ← already idiomatic
```

### Core (ren Kotlin — ingen framework, ingen `java.sql`)

```kotlin
interface Transaksjon

interface TaskProdusent {
    fun <P : Any> opprett(
        transaksjon: Transaksjon,
        type: TaskType<P>,
        payload: P,
        triggerTid: Instant? = null,
    ): TaskId

    fun <P : Any> opprettFrittstående(
        type: TaskType<P>,
        payload: P,
        triggerTid: Instant? = null,
    ): TaskId
}
```

(`TaskType<P>` er den typede nøkkelen et `TaskStep<P>` deklarerer — typede payload-er
designes separat; opprettelse er typesikker fra dag én.)

### Postgres-modul

```kotlin
class JdbcTransaksjon(internal val connection: Connection) : Transaksjon
```

Repositoryet pakker ut med en require-cast (`krever JdbcTransaksjon`), skriver til
kallerens connection, og **committer eller lukker den aldri** — transaction eies av den
som åpnet den.

### Spring Boot starter — gjenoppretter ambient-idiomet, feiler tydelig

```kotlin
class TaskService(
    private val dataSource: DataSource,
    private val produsent: TaskProdusent,
) {
    fun <P : Any> opprett(type: TaskType<P>, payload: P): TaskId {
        check(TransactionSynchronizationManager.isActualTransactionActive()) {
            "Task må opprettes i en aktiv transaksjon (outbox-garantien). Kall fra @Transactional."
        }
        val connection = DataSourceUtils.getConnection(dataSource)
        try {
            return produsent.opprett(transaksjon = JdbcTransaksjon(connection), type = type, payload = payload)
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource)
        }
    }
}
```

`DataSourceUtils.getConnection` returnerer den transaction-bundne connectionen både under
Spring Data JDBC og JPA (`JpaTransactionManager` eksponerer samme connection). EF-kallsteder
beholder dagens ergonomi:

```kotlin
@Transactional
fun fattVedtak(vedtak: Vedtak) {
    vedtakRepository.insert(vedtak)
    taskService.opprett(type = SendVedtaksbrev.TYPE, payload = VedtaksbrevPayload(vedtak.id))
}
```

### Ktor / Gjenny — ingen adapter-magi trengs

```kotlin
dataSource.transaction { connection ->
    vedtakDao.lagre(connection = connection, vedtak = vedtak)
    taskProdusent.opprett(
        transaksjon = JdbcTransaksjon(connection),
        type = SendVedtaksbrev.TYPE,
        payload = VedtaksbrevPayload(vedtak.id),
    )
}
```

## Engine-siden av outboxen (beslutning 2)

Plukking og kjøring er **to separate transactions**, med vilje:

1. **Plukk** — `SKIP LOCKED` CTE-updaten flipper rader til `KJØRER` og committer
   umiddelbart (den commiten er det som får andre poder til å hoppe over dem). Setter
   `plukket_tid`.
2. **Kjøring** — engine åpner en fersk transaction per task:

```kotlin
repo.iEgenTransaksjon { tx ->
    val kontekst = TaskKontekst(transaksjon = tx, task = task, produsent = produsent)
    step.utfor(kontekst)
    repo.markerFullført(transaksjon = tx, id = task.id)
}   // commit — eller rollback ved exception, deretter egen liten tx for feil-bokføring
```

Dette gir:

- **Atomiske kjeder** — `kontekst.opprettNesteTask(...)` legger neste task i kø på samme
  transaction som `FULLFØRT`-oppdateringen; ingen luke der step A ble ferdig, men step B
  aldri ble opprettet.
- **Ryddige retries** — DB-skrivene til et step rulles tilbake sammen med det; et retry
  dupliserer ikke databaseeffekter. Eksterne kall (send brev, …) forblir at-least-once;
  det er iboende og forblir dokumentert.
- **Én mental modell** — skriv går sammen med en `Transaksjon`; den som åpnet den,
  committer den.

Hvis en pod dør midt i et step, blir raden stående som `KJØRER`; reaperen tar den tilbake
via `plukket_tid`-timeouten. (Reaper er engine-core, se plan-dokumentet.)

**Kostnad, akseptert:** et step holder en pooled connection gjennom hele kjøringen —
`maxSamtidighet` må dimensjoneres mot connection poolen, og langvarige step bør gjøre
trege eksterne kall før DB-skrivene sine der det er mulig.

## Invarianter (angitt én gang, håndhevet overalt)

- Produsent-API-et er **blocking, ikke `suspend`** — Spring-transactions er
  trådbundet; en suspending opprettelse kunne fortsatt på en annen tråd og stille koblet
  seg fra transactionen. Inserten er et sub-millisekund-skriv på en connection kalleren
  allerede holder.
- Task-tabellene ligger **i hver vertsapps egen database**. Det finnes ingen sentral
  prosessering-DB; hver app bygger inn sin egen kø.
- Ingen kodevei åpner noen gang en skjult transaction på vegne av `opprett`. Å velge
  bort outbox skrives `opprettFrittstående`.

## Åpne tråder (neste økter)

- `TaskStep<P>` / `TaskType<P>`-design: payload-(de)serialisering, per-step config
  (maxAntallFeil, backoff, settTilManuellOppfølging), step registry.
- `TaskKontekst`-flate: nøyaktig hva et step kan nå (opprettNesteTask, metadata,
  logger, …).
- `TaskRepository`-portsignaturer formet av denne beslutningen (claim / iEgenTransaksjon /
  markerFullført / feil-bokføring).
