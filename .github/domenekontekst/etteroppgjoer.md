# Etteroppgjør (OMS)

Årlig etterskuddsvis kontroll av om bruker fikk riktig omstillingsstønad i det foregående inntektsåret. Gjelder kun OMS – ikke BP.

> **Overordnet flyt:** Se [saksgangen.md](saksgangen.md) "Flyt 4 – Etteroppgjør" for prosessen fra skatteoppgjør til ferdigstilt vedtak.

## Ansvarsområder

- Motta skatteoppgjørhendelser fra Sigrun og opprette etteroppgjør per sak
- Forbehandling: hente PGI og A-inntekt, beregne avvik, sende forhåndsvarsel
- Håndtere svarfrist, brukers tilbakemelding og omgjøring
- Opprette revurdering når det er nødvendig

## Sentrale begreper

| Begrep | Forklaring |
|---|---|
| `inntektsaar` | Året vi gjør opp (f.eks. 2024). Etteroppgjøret behandles påfølgende år. |
| `Etteroppgjoer` | Overordnet statussporingsobjekt – ett per sak per inntektsår. |
| `EtteroppgjoerForbehandling` | Arbeidsobjektet der saksbehandler behandler etteroppgjøret og der etteroppgjørsdata lagres. |
| Forhåndsvarsel | Brev til bruker med beregnet avvik og faktisk inntekt. Svarfrist 1 måned. |
| PGI | Pensjonsgivende inntekt fra Sigrun – eneste juridisk korrekte grunnlag for beregningen. |
| A-inntekt | Detaljert inntektsdata fra A-ordningen – vises til saksbehandler som referanse, brukes IKKE i beregningen. |

## Trigger

Skatteoppgjørhendelser fra Skatteetaten (via Sigrun) mottas gjennom polling (feature-togglet). Saker som ikke har mottatt skatteoppgjør innen desember fanges opp av en jobb som setter status `MANGLER_SKATTEOPPGJOER` og oppretter oppgave med flagg `mottattSkatteoppgjoer = false`.

## Inntektskilder – PGI vs. A-inntekt

Data hentes fra to kilder, men begge er referansepunkter – **ingen er fasit**. Saksbehandler fastsetter faktisk inntekt.

**PGI (Sigrun)** – aggregerte årsbeløp per inntektstype etter skatteloven. Eneste juridisk korrekte grunnlag for beregningen.

**A-ordningen** – detaljerte enkeltutbetalinger med dato, arbeidsgiver og metadata. Bredere inntektsbegrep, inkl. ikke-pensjonsgivende poster. Kun visning for saksbehandler.

Disse kildene er ikke direkte sammenlignbare – PGI er pensjonsgivende inntekt per skatteloven, A-inntekt dekker et videre spekter av utbetalinger.

## Forbehandling vs. revurdering

Forbehandlingen er et *preview* av det endelige vedtaket – den viser saksbehandler og bruker hva utfallet vil bli, men er ikke et vedtak i seg selv.

Når revurdering opprettes, kopieres den ferdigstilte forbehandlingen (`kopiertFra != null`). Kopien er datakontainer for revurderingen, har ingen oppgave, og ferdigstilles automatisk ved iverksettelse.

**Flere innvilgede perioder:** Ett etteroppgjør dekker ett inntektsår, også når året har opphørsperioder med senere gjenopptak. Forbehandlingens `innvilgetPeriode` er da yttergrensene for oppgjøret, ikke en sammenhengende innvilgelse. De faktiske innvilgede periodene avgrenses til året og sendes til beregning, som kontrollerer dem mot siste årsoppgjørs `ytelseFoerAvkorting`. Avvik blokkerer beregningen; perioder eller inntekt autokorrigeres ikke. Innvilgede måneder og ytelse i hull håndteres av eksisterende avkortingsregler. Frontend viser periodene fra forbehandlingens tidligere avkorting, også ved 0 kr etter inntektsavkorting.

**Feature toggle:** Flerperiodestøtten styres av `beregn_over_flere_perioder`, med standardverdi `false`. Bryteren må være aktiv for behandling, beregning og UI/BFF i miljøet der funksjonen skal brukes. Når den er av, blokkeres opprettelse og ny inntektsberegning av flerperiode-etteroppgjør, også for eksisterende forbehandlinger og eldre beregningskall uten perioder i request. Ordinær BP/OMS-beregning med flere vedtaksperioder blokkeres også, i stedet for å falle tilbake til beregning som kan fylle opphørsgap. Sammenhengende oppgjør fungerer som før, også når G-regulering deler beregningen i flere perioder. Historiske data og korrekthetssperrer beholdes uavhengig av bryteren.

**Endring til ugunst**: Hvis saksbehandler registrerer at brukers svar gir endring til ugunst (`endringErTilUgunstForBruker = JA`) → revurderingen avsluttes og ny forbehandling opprettes fra bunnen. Bruker må få nytt forhåndsvarsel med oppdaterte tall (lovkrav).

**Avbrutt forbehandling**: Tilbakestiller etteroppgjøret til `MOTTATT_SKATTEOPPGJOER`. Kan ikke gjenopptas – saksbehandler må opprette ny.

## Spesialtilfeller

**Dødsfall i inntektsåret** – ingen etteroppgjør, ferdigstilles uten brev.

**Dødsfall etter inntektsåret** – etteroppgjøret gjennomføres normalt, men tilbakekreving overstyres til `INGEN_ENDRING` (NAV-policy: uhensiktsmessig å kreve fra dødsbo). Etterbetaling utbetales til dødsboet.

**Svarfrist** – produksjon: 1 måned (`EN_MND`) = 3 uker lovfestet + postgang. Test: `ETT_MINUTT`/`FEM_MINUTT`.

## Nøkkelklasser

- `EtteroppgjoerService` (`etterlatte-behandling`) – oppretter og oppdaterer etteroppgjør, koordinerer statusoverganger
- `EtteroppgjoerForbehandlingService` (`etterlatte-behandling`) – oppretter, beregner og ferdigstiller forbehandlinger
- `EtteroppgjoerRevurderingService` (`etterlatte-behandling`) – oppretter revurdering basert på ferdigstilt forbehandling
- `PensjonsgivendeInntektService` (`etterlatte-behandling`) – henter PGI fra Sigrun
- `InntektskomponentService` (`etterlatte-behandling`) – henter A-inntekt fra A-ordningen
- `EtteroppgjoerService` (`etterlatte-beregning`) – beregner avvik mellom faktisk og utbetalt (se [beregning.md](beregning.md))

## Avhengigheter

**Kaller:**
- `etterlatte-beregning` – `beregnAvkortingForbehandling`, se [beregning.md](beregning.md)
- Sigrun – PGI via polling (`LesSkatteoppgjoerHendelserJobService`, cursor-basert)
- Inntektskomponenten – A-inntekt

**Lytter på:** Skatteoppgjørhendelser via polling (feature-togglet)
