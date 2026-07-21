# efterlatte-prosessering — dokumentasjon

Ren gjenoppbygging av NAVs `familie-prosessering`: en **transaksjonell-outbox
jobbkø** for Kotlin, framework-agnostisk, som apper kan embedde som et bibliotek.

> **Efterlatte** = EF (Enslig Forsørger) + Etterlatte — de to sammenslåtte teamene.

---

## Status (2026-07)

> **Biblioteket er kuttet ut (Fase 5, 2026-07-21).** Koden bor nå i det frittstående repoet
> **`navikt/efterlatte-prosessering`** (`prosessering-{core,postgres,ktor}`), sammen med disse
> docs-ene og domeneekspert-agenten. Videre arbeid på selve biblioteket skjer der. Denne
> kopien i Gjenny beholdes for host-domenet (skygge-task, outbox-bro, admin-routes) som blir
> værende her til Gjenny bytter til den publiserte artefakten. Se «Fase 5» i veikartet.

Vi har snevret inn fra den brede visjonen til en **fokusert PoC**: få **Gjenny**
(`pensjon-etterlatte-saksbehandling`, Ktor) til å ta imot søknader på en
prosesserings-måte — en **skyggekjøring** som beviser reliable/retryable/observerbar
task-håndtering *inne i en Ktor-app*, uten å faktisk opprette behandling.

Beslutninger for PoC-en:
- **Kun Gjenny (Ktor)** nå. Spring-adapter, TCK og multi-backend-UI er utsatt.
- **Frontend ignoreres** inntil videre.
- **Tynt bibliotek** Gjenny drar inn. ef-sak kan følge senere.
- **Prototypet inne i Gjenny først** (rask iterasjon), nå kuttet ut til eget repo.

Veien dit står i **[05-poc-veikart.md](05-poc-veikart.md)**.

---

## Les i denne rekkefølgen

| # | Fil | Hva | Status |
|---|-----|-----|--------|
| 1 | [01-intensjon.md](01-intensjon.md) | *Hvorfor* — kontekst, hvem vi er, låste beslutninger | Gjeldende |
| 2 | [02-arkitektur.md](02-arkitektur.md) | Arkitektur, engine-interne detaljer, modulskisse | Gjeldende (målbilde) |
| 3 | [04-outbox-api.md](04-outbox-api.md) | Produsent-API — hvordan en task opprettes i vertens transaksjon | Gjeldende |
| 4 | [05-poc-veikart.md](05-poc-veikart.md) | Skyggekjøring-PoC: scope, faser, neste steg | **Aktiv** |

Historikk ligger i [arkiv/](arkiv/) (opprinnelig framing).

> **Frontend/operatør-UI er utenfor scope inntil videre** og ikke helt avklart. Den gamle
> funksjonsspec-en og frontend-kickoff-en ligger parkert i det frittstående
> `efterlatte-prosessering`-repoet (`docs-frontend/`, `contract/`, `frontend/`).

---

## Viktige begreper (kort)

- **Transaksjonell outbox** — task-raden skrives i *samme DB-transaksjon* som
  forretnings-skrivet, som garanterer at arbeidet kjøres minst én gang. Dette er grunnen
  til at motoren **må** være et embedded bibliotek, ikke en egen tjeneste.
- **5-status-modellen**: `KLAR`, `KJØRER`, `FULLFØRT`, `STOPPET`,
  `AVBRUTT`. Implementert i `core` (Fase 0 gjort — se veikartet).
- **Skyggekjøring** — task-steget *simulerer* søknadsmottak (validerer/logger) uten å
  kalle behandling. Beviser motor + observerbarhet før vi kobler på ekte effekter.

## Historisk kontekst

`familie-prosessering` ([backend](https://github.com/navikt/familie-prosessering-backend),
[frontend](https://github.com/navikt/familie-prosessering-frontend)) er verktøyet EF/BAKS
bruker i dag: en Spring Boot-basert (Java→Kotlin-portert) task-kø. Gjenny kan ikke ta den
i bruk som den er. Denne dokumentasjonen beskriver en ren gjenoppbygging.
