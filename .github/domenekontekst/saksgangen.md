# Saksgangen i Gjenny

## Grunnbegreper

**Sak** identifiserer én ytelse for én person. En person kan ha én BP-sak og én OMS-sak, aldri to av samme type.

**Behandling** er alltid knyttet til en sak og representerer én endring av ytelsen. All historikk, vedtak og utbetalingsendringer skjer via behandlinger. Behandlingstyper: `FØRSTEGANGSBEHANDLING` og `REVURDERING` (inkl. opphør).

Tilbakekrevings- og klagebehandlinger er egne konstrukter, men knyttet til samme sak.

---

## Statussekvens for en behandling

```
OPPRETTET
  → VILKAARSVURDERT          (vilkårsvurdering gjennomført)
  → TRYGDETID_OPPDATERT      (trygdetid fastsatt)
  → BEREGNET                 (ytelse beregnet)
  → AVKORTET                 (kun OMS – inntektsavkorting anvendt)
  → FATTET_VEDTAK            (saksbehandler sender til attestering)
  → ATTESTERT                (en annen saksbehandler godkjenner)
  → [TIL_SAMORDNING          (OMS – avventer samordning mot TP)]
  → [SAMORDNET               (samordning ferdig)]
  → IVERKSATT                (utbetaling og brev trigges)
```

Sidespor:
- `RETURNERT` – attestant sender tilbake til saksbehandler
- `AVSLAG` – attestert, men vedtakstype er avslag
- `ATTESTERT_INGEN_ENDRING` – revurdering der beregnet beløp ikke endret seg
- `AVBRUTT` – behandlingen kanselleres

---

## Flyt 1 – Ny søknad (førstegangsbehandling)

**Trigger:** Søknad innsendt digitalt → hendelse på Rapids → `etterlatte-behandling-kafka` oppretter sak og behandling.

1. Saksbehandler åpner behandlingen i Gjenny, gjennomgår vilkårsvurdering
2. Trygdetid fastsettes i `etterlatte-trygdetid` (evt. importert fra Pesys)
3. Ytelsen beregnes i `etterlatte-beregning`; OMS avkortes mot oppgitt inntekt
4. Saksbehandler fatter vedtak → REST-kall mot `etterlatte-vedtaksvurdering`
5. Attestant godkjenner → behandling settes `ATTESTERT`
6. OMS-saker med samordning: `TIL_SAMORDNING` → Pesys svarer → `SAMORDNET`
7. `IVERKSATT`: vedtaksvurdering publiserer hendelse på den interne Rapids-en
   - `etterlatte-utbetaling` lytter på `ATTESTERT`-hendelsen og oppretter utbetalingsoppdrag → Oppdrag via IBM MQ
   - `etterlatte-brev-kafka` lytter på Rapids-en og kaller `etterlatte-brev-api` for å generere og distribuere vedtaksbrev
   - Vedtakshendelser publiseres også til `etterlatte.vedtakshendelser`-topicet for eksterne konsumenter

---

## Flyt 2 – Revurdering (manuell)

**Trigger:** Saksbehandler oppretter revurdering i Gjenny, eller en grunnlagsendringshendelse oppretter en oppgave som leder til revurdering.

Statussekvensen er den samme som for førstegangsbehandling.

Vanlige revurderingsårsaker per ytelsestype:

| Årsak | BP | OMS |
|---|:---:|:---:|
| `NY_SOEKNAD` | ✓ | ✓ |
| `INNTEKTSENDRING` | | ✓ |
| `AARLIG_INNTEKTSJUSTERING` | | ✓ |
| `INSTITUSJONSOPPHOLD` | ✓ | ✓ |
| `YRKESSKADE` | ✓ | ✓ |
| `SOESKENJUSTERING` | ✓ | |
| `FORELDRELOES` | ✓ | |
| `SIVILSTAND` (opphør) | | ✓ |
| `ALDERSOVERGANG` (opphør) | ✓ | ✓ |
| `DOEDSFALL` (opphør) | ✓ | ✓ |
| `ETTEROPPGJOER` | | ✓ |

Revurderinger med `skalSendeBrev = false` (f.eks. `REGULERING`, `ALDERSOVERGANG`) genererer ikke brev automatisk.

---

## Flyt 3 – Automatisk omregning (G-regulering / inntektsjustering)

**Trigger:** `etterlatte-tidshendelser` publiserer en tidsbasert hendelse på Rapids-en.

- **G-regulering** (slutten av mai): Alle løpende saker får en automatisk revurdering med ny G-sats. Kjøres som masseoperasjon.
- **Årlig inntektsjustering** (november, kun OMS): Viderfører brukers inntektsopplysninger til neste år.

Hendelsen konsumeres av `etterlatte-vedtaksvurdering-kafka` (og evt. `etterlatte-beregning-kafka`), som kaller tilhørende REST-apper for å gjennomføre og iverksette revurderingen automatisk.

---

## Flyt 4 – Etteroppgjør (kun OMS)

Gjennomføres etter at skatteoppgjøret for et ytelsesår er ferdig. Primærtrigger er skatteoppgjørshendelser fra Skatteetaten. Saker som ikke har mottatt skatteoppgjør innen desember fanges opp av en jobb som setter status `MANGLER_SKATTEOPPGJOER` og presser dem videre med et eget flagg.

**Steg 1 – Forbehandling**
- Data hentes fra Skatteetaten (pensjonsgivende inntekt) og A-ordningen – begge er referansepunkter, ingen er fasit
- Saksbehandler fastsetter faktisk inntekt og beregner differansen mot utbetalt stønad
- Informasjonsbrev (ingen avvik) eller varselbrev (avvik) sendes til bruker
- Ferdigstilling av forbehandlingen setter automatisk status `VENTER_PAA_SVAR`

**Steg 2 – Revurdering med vedtak**
- Revurderingen kopierer forbehandlingen (`kopiertFra`); forbehandlingen er et preview av det endelige vedtaket
- Saksbehandler kan korrigere faktisk inntekt basert på brukers tilbakemelding
- Vedtaket slår fast om bruker skal etterbetales, tilbakekreves, eller ingen endring
- Normal vedtaksflyt: fatte → attestere → iverksette

> `EtteroppgjoerGrense` avgjør utfallet (etterbetaling/tilbakekreving/ingen endring), ikke om vi trigger revurdering.
> Beregning av restanse i avkortingen er reelt kompleks – se ikke etter en forenkling som ikke er der.

Se [etteroppgjoer.md](etteroppgjoer.md) for detaljert flyt, spesialtilfeller og nøkkelklasser.

---

## Flyt 5 – Tilbakekreving

**Trigger:** En revurdering reduserer ytelsen for perioder der det allerede er utbetalt.

- `etterlatte-tilbakekreving` mottar melding fra tilbakekrevingskomponenten og oppretter en tilbakekrevingsbehandling på saken
- Saksbehandler vurderer om bruker skal betale tilbake, hvorfor, og hvor stor andel
- Vedtak fattes og attesteres; `etterlatte-tilbakekreving` sender resultat til tilbakekrevingskomponenten og kvitterer

---

## Flyt 6 – Klage

**Trigger:** Bruker klager på et vedtak.

1. Klagen registreres i Gjenny via `etterlatte-klage`
2. Saksbehandler vurderer om klagen oppfyller formkravene
3. **Tatt til følge:** Ny revurdering opprettes på saken → normal vedtaksflyt
4. **Stadfestet:** Opprinnelig vedtak opprettholdes → klagen sendes til Kabal (klageinstansen)
5. `etterlatte-klage` lytter på Kabal-hendelser; når klagen er ferdigbehandlet opprettes en oppgave til saksbehandler i Gjenny

---

## Viktige tverrgående regler

- **Ferdig behandlede vedtak er uforanderlige.** Statuser `IVERKSATT`, `ATTESTERT`, `SAMORDNET` kan ikke endres. All ny endring skjer via ny behandling.
- **Revisjonslogg.** Alle statusoverganger og hvem som utfører dem logges. Dette er et lovkrav for internkontroll og revisjon.
- **Behandling er autoriteten på tilgang.** Alle tjenester som skal gjøre noe med en sak/behandling/person *skal* spørre `etterlatte-behandling` om tilstand og tilgang først.
- **Oppgaver** opprettes ved behandlingsstart og ferdigstilles ved iverksetting. Grunnlagsendringer (PDL-hendelser, Joark-journalføringer) kan også opprette oppgaver uten at det er en åpen behandling.
