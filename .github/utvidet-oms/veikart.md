# Veikart – utvidet omstillingsstønad

Felles oversikt over hva som skal gjøres for søknaden om utvidet (forlenget) omstillingsstønad, og hva som er gjort. Arbeidet går på tvers av tre repoer.

| Fil | Brukes til |
|-----|-----------|
| `veikart.md` (denne) | Hva skal gjøres, i hvilken rekkefølge, og hvor står vi |
| [`beslutninger.md`](beslutninger.md) | Hvorfor ting er løst som de er, og hva som ikke er avklart ennå |
| [`spesifikasjon.md`](spesifikasjon.md) | Hvordan søknaden skal oppføre seg: felt, tekster, validering og kontrakt |
| [`../agents/utvidet-oms.md`](../agents/utvidet-oms.md) | Agent som jobber ut fra disse filene og holder dem oppdatert |

**Status ligger i Favro.** Veikartet lenker til kortene og viser milepæler. Status per kort skal ikke vedlikeholdes to steder.

Statusoversikt over branchene: `.github/scripts/utvidet-oms-status.sh [--fetch]`

### Komme i gang
- **I saksbehandling:** Start `copilot` og kjør `/agent utvidet-oms`.
- **I pensjon-etterlatte eller felles:** Start `copilot`, kjør `/add-dir ../pensjon-etterlatte-saksbehandling` og deretter `/agent utvidet-oms`.
- Si for eksempel «Hvor står vi?» eller «Ta neste arbeidspakke».

---

## Mål

Brukere som allerede har omstillingsstønad skal kunne søke om å forlenge stønaden i inntil 2 år når de tar utdanning eller deltar i arbeidsrettet tiltak. Søknaden skal fungere som de andre søknadene våre, men bare inneholde det den trenger.

## Flyt på tvers av repoene

```
utvidet-omstillingsstoenad-ui (pe)
  └─ etterlatte-node-server (pe) ─ BFF
       └─ innsendt-soeknad (pe) ─ lagrer kladd/søknad, publiserer soeknad_innsendt
            └─ Kafka-rapid
                 └─ NySoeknadRiver (sb, etterlatte-behandling-kafka)
                      ├─ finner/oppretter OMS-sak
                      ├─ journalfører (PDF fra ey-pdfgen i felles)
                      └─ oppretter oppgave (midlertidig, se B4)
Kontrakt: UtvidetOmstillingsstoenad i pensjon-etterlatte-felles/common
```

`pe` = pensjon-etterlatte, `sb` = pensjon-etterlatte-saksbehandling, `felles` = pensjon-etterlatte-felles

## Aktive brancher

| Repo | Branch | PR | Innhold |
|------|--------|----|---------|
| pe | `utvidet-soknad-struktur-ny-app` | – | Skjelett for `apps/utvidet-omstillingsstoenad-ui` |
| pe | `NAV-31200-sende-inn-utvidet-oms-soeknad` | [#2531](https://github.com/navikt/pensjon-etterlatte/pull/2531) | innsendt-soeknad tar imot den nye søknadstypen |
| sb | `nav-31200-ta-imot-soknad-utvidet-oms` | [#8971](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/8971) | Mottak i `NySoeknadRiver` og journalføring |

Merget: felles `utvidet-omstillingsstoenad-kontrakt` ([#620](https://github.com/navikt/pensjon-etterlatte-felles/pull/620)) gir `common` `2026.10.09-12.12.efc9bddf6546`.

Oppdater tabellen når en branch merges eller en ny startes.

---

## Faser og arbeidspakker

Status: ✅ ferdig · 🚧 pågår · ⏳ ikke startet · ⛔ blokkert (se beslutninger)

### Fase 0 – Oppsett

| # | Arbeidspakke | Repo | Favro | Status |
|---|--------------|------|-------|--------|
| 0.1 | Veikart, beslutninger, spesifikasjon, agent og statusskript | sb | – | 🚧 |

### Fase 1 – Kontrakt og PDF

| # | Arbeidspakke | Repo | Favro | Status |
|---|--------------|------|-------|--------|
| 1.1 | Kontrakt v1 (`UtvidetOmstillingsstoenad`, `GjenlevendeUtvidetOMS`, `SoeknadType`) | felles | NAV-31301 | ✅ [#620](https://github.com/navikt/pensjon-etterlatte-felles/pull/620) |
| 1.2 | Avklare avvik mellom Figma og kontrakt (Å1, Å2, Å5). Justere kontrakt og eksempler ved behov, og publisere en ny versjon. | felles | | ⏳ |
| 1.3 | Bumpe `common` til `2026.10.09-12.12.efc9bddf6546` i pe og sb | pe, sb | NAV-31200 | ✅ på branchene til 2.1 og 2.3 |
| 1.4 | PDF-mal `utvidet_omstillingsstoenad_v1` i ey-pdfgen (mal og testdata) | felles | | ⏳ |

### Fase 2 – Backend

| # | Arbeidspakke | Repo | Favro | Status |
|---|--------------|------|-------|--------|
| 2.1 | innsendt-soeknad tar imot den nye søknadstypen (adressebeskyttelse og fixtures) | pe | NAV-31200 | 🚧 [#2531](https://github.com/navikt/pensjon-etterlatte/pull/2531) |
| 2.2 | Mock i node-server for `har-loepende-sak`, person og kladd | pe | | ⏳ |
| 2.3 | Mottak i Gjenny: journalføring og oppgave | sb | NAV-31200 | 🚧 [#8971](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/8971), journalføring krever 1.4 |
| 2.4 | Behandlingsløp i Gjenny (revurdering?) | sb | | ⛔ Å8 |

### Fase 3 – Søknaden (`apps/utvidet-omstillingsstoenad-ui`)

Én PR per arbeidspakke. Hver pakke har i18n (nb/nn/en), validering og vitest-tester. Spesifikasjonen er fasit for felt, tekster og regler.

| # | Arbeidspakke | Favro | Status |
|---|--------------|-------|--------|
| 3.0 | Skjelett for appen: forside, kvittering, kladd, i18n, nais dev og workflow | | ✅ på branch |
| 3.1 | Grunnmur: stegdefinisjon og ruter, fremdrift, Forrige/Neste/Avbryt, react-hook-form per steg, lagring ved stegbytte, frontendmodell og mapping til kontrakten (testet mot eksempel-JSON fra felles) | | ⏳ |
| 3.2 | Steg 1 Introside: sjekk av sak, to varianter, samtykke | | ⏳ |
| 3.3 | Steg 2 Personopplysninger | | ⏳ |
| 3.4 | Steg 3 Situasjonen din i dag | | ⏳ |
| 3.5 | Steg 4 Utdanning og tiltak | | ⏳ |
| 3.6 | Steg 5 Inntekt | | ⏳ |
| 3.7 | Steg 6 Dokumentasjon (tom side inntil videre) | | ⏳ Å4 |
| 3.8 | Steg 7 Oppsummering og innsending | | ⏳ |
| 3.9 | Steg 8 Kvittering etter Figma | | ⏳ |
| 3.10 | Cypress-flyt og axe gjennom hele søknaden | | ⏳ |

### Fase 4 – Ende-til-ende og prod

| # | Arbeidspakke | Repo | Favro | Status |
|---|--------------|------|-------|--------|
| 4.1 | Ende-til-ende-test i dev: søknad → journalpost → oppgave i Gjenny | alle | | ⏳ |
| 4.2 | Prodklargjøring: feature toggle, prod-nais, inngang fra nav.no | pe | | ⛔ Å9 |

### Avhengigheter

```
1.4 → 2.3 (journalføring feiler uten PDF-mal)
1.2 → ny common-versjon → bump i pe og sb (bare hvis kontrakten endres)
3.1 → 3.2 … 3.9 → 3.10
2.2 → 3.2
1.2 → 3.8 (mapping må treffe endelig kontrakt)
2.1 + 2.3 + 3.8 → 4.1
```

---

## Logg

Kort logg over milepæler. Nyeste øverst. Én linje per hendelse, med lenke til PR der det finnes.

- **2026-10-20** – Veikart, beslutninger, spesifikasjon og agenten `utvidet-oms` opprettet i saksbehandling.
- **2026-10** – PR-er åpnet for mottak: pe [#2531](https://github.com/navikt/pensjon-etterlatte/pull/2531) og sb [#8971](https://github.com/navikt/pensjon-etterlatte-saksbehandling/pull/8971). Skjelett for ny app på branch i pe.
- **2026-10-09** – Kontrakt v1 merget i felles ([#620](https://github.com/navikt/pensjon-etterlatte-felles/pull/620)).
