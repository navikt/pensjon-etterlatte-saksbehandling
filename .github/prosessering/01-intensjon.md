# Efterlatte-prosessering — intensjon & kickoff

> Lim dette inn i en ny chat for å fortsette sparringen med full kontekst.
> Det fanger *hva* vi gjør, *hvorfor*, og *hvor vi er* — samme ånd som den
> opprinnelige framingen, bare strammet inn.

---

## Konteksten (hvem vi er)

To NAV-team har slått seg sammen til ett bredt domene:
- **Enslig Forsørger (EF)** — folketrygdloven **kap. 15**. Apper som `familie-ef-sak`
  og under-appene. Stack: **Spring Boot**.
- **Etterlatte** — folketrygdloven **kap. 17**. Det store saksbehandlingssystemet **Gjenny**
  og appene rundt. Stack: **Ktor**, med en **rapids-and-rivers**-tilnærming.

Begge teamene bygger backendene sine i **Kotlin** — det er den felles grunnen.

## Hva vi tenker nytt om, og hvorfor

EF (sammen med søsterteamet BAKS) har lenge brukt **`familie-prosessering`** — et
task/jobb-håndteringsverktøy. Vi er glad i det fordi det lar oss batche arbeid som må
gjøres, og når noe feiler sier det ifra og lar oss ofte bare **«prøv igjen»** (rekjøre). Men:
- Det er gammelt — et Java-repo konvertert til Kotlin.
- Det er **kun Spring Boot**, så Gjenny kan ikke ta det i bruk som det er.
- Teknisk leder ser det med rette som mer et **bibliotek/API enn en app** — og vi er enige.

Kjernekonseptet er ikke nytt (det overlapper med Kotlins coroutine-«Job»-verden, som Gjenny
allerede bruker). Så i stedet for å dra det gamle med oss, vil vi ha en **ren start**.

## Målet

En greenfield, **framework-agnostisk Kotlin-task-kø** som **både Spring Boot- (EF) og
Ktor- (Gjenny) apper kan embedde**. Lettvekt, eid, vedlikeholdbart — et mesterverk i ren
arkitektur, ikke en port. Sikkerhet og fornuftig bruk som internt verktøy bygget inn fra
start.

## Den bærende innsikten

`prosessering` er egentlig en **transaksjonell-outbox jobbkø**: en task-rad skrives *i
samme DB-transaksjon* som forretningsendringen, og garanterer minst-én-gang-kjøring. Det
betyr at motor/produsent/persistens **må være et embedded bibliotek** (en frittstående
tjeneste ville gjeninnføre dual-write-problemet). `rapids-and-rivers` er et **komplement,
ikke en erstatning** — events kontra pålitelig, operatør-observerbart arbeid.

## Låste beslutninger (fra sparringen 2026-05-21)

- **Navn:** `efterlatte-prosessering` (EF + Etterlatte satt sammen).
- **Motor:** bygg en fokusert kjerne; lån db-schedulers `SELECT … FOR UPDATE SKIP
  LOCKED`-teknikk **uten** avhengigheten.
- **Schema/kontrakt:** rent brudd + en engangsmigrering for EF.
- **Adaptere:** både **Spring Boot starter** og **Ktor plugin** fra dag én, holdt ærlige
  av et felles kontrakt-testkit (TCK).
- **Kjernen forblir framework-agnostisk** uansett (gjør den også Spring-Modulith-vennlig).

## Målbilde for modulform (mono-repo)

`core` · `postgres` · `api` (OpenAPI) · `tck` · `spring-boot-starter` · `ktor`

## Hvor PoC-en står (bygget 2026-05-21, i `~/efterlatte-prosessering/`)

- **`contract/`** — `openapi.yaml` + `mock-data.json` (kuttpunktet begge sporene deler).
- **`backend/`** — Kotlin/Gradle enkeltmodul: coroutine-motor + Postgres `SKIP LOCKED`.
  **Konkurransebevis passerer**: 4 motorer, 1000 tasks, **null dobbeltkjøring** (verifisert
  i minne *og* via en DB UNIQUE-constraint). Retry→`MANUELL_OPPFOLGING`- og
  retry→`FERDIG`-stiene passerer. (`cd backend && ./gradlew test`)

> Frontend/operatør-UI er parkert utenfor PoC-scope inntil videre — se det frittstående
> `efterlatte-prosessering`-repoet (`contract/`, `frontend/`, `docs-frontend/`).

## Åpne tråder å sparre på videre

- Sikkerhetsdybde: Azure AD/Entra for operatører, TokenX, AD-gruppe-gating, audit-logging,
  payload-sensitivitet (personnummer / kap. 15+17-data), retention.
- Å gå fra enkeltmodul-PoC-en til den fulle modulstrukturen.
- Deployment (NAIS), observerbarhet (Micrometer), leader election (Postgres advisory lock).
- Migreringsmekanikk for `familie-ef-sak` (gammelt schema → nytt).
