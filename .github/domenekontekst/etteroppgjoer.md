# Etteroppgjør (OMS)

Årlig etterskuddsvis kontroll av om bruker fikk riktig omstillingsstønad i det foregående inntektsåret. Sammenligner faktisk inntekt fra skatteoppgjøret med avkortingsgrunnlaget som ble brukt i utbetalingene. Gjelder kun OMS – ikke BP.

## Ansvarsområder

- Motta skatteoppgjørhendelser fra Sigrun og opprette etteroppgjør per sak
- Forbehandling: hente PGI og A-inntekt, beregne avvik, sende forhåndsvarsel
- Håndtere svarfrist, brukers tilbakemelding og omgjøring
- Opprette revurdering ved tilbakekreving eller etterbetaling

## Sentrale begreper

| Begrep | Forklaring |
|---|---|
| `inntektsaar` | Året vi gjør opp (f.eks. 2024). Etteroppgjøret behandles påfølgende år. |
| `Etteroppgjoer` | Overordnet statussporingsobjekt – ett per sak per inntektsår. |
| `EtteroppgjoerForbehandling` | Arbeidsobjektet der saksbehandler behandler etteroppgjøret og der etteroppgjørsdata lagres. |
| Forhåndsvarsel | Brev til bruker med beregnet avvik og antatt inntekt. Svarfrist 1 måned. |
| PGI | Pensjonsgivende inntekt fra Sigrun – det eneste beregningsgrunnlaget. |
| A-inntekt | Detaljert inntektsdata fra A-ordningen – vises til saksbehandler som referanse, men brukes IKKE i beregningen. |

## Overordnet flyt

Flyten har to hovedsteg: først en forbehandling der avviket kartlegges og bruker varsles, deretter en revurdering dersom det er avvik som krever vedtak.

1. Skatteoppgjørhendelse mottas fra Sigrun (polling, feature-togglet) → `Etteroppgjoer` opprettes
2. Oppgave opprettes for saksbehandler
3. Saksbehandler oppretter forbehandling manuelt – ikke automatisk fordi det ville blokkere pågående behandlinger i saken
4. PGI og A-inntekt hentes, beregning kjøres mot hva som ble utbetalt
5. Forhåndsvarsel sendes til bruker, svarfrist 1 måned
6. Svarfrist utløper eller bruker svarer med ny informasjon
7. Resultat: **TILBAKEKREVING/ETTERBETALING** → revurdering → normal vedtaksflyt → `FERDIGSTILT`, eller **INGEN_ENDRING** → `FERDIGSTILT` direkte

## Inntektskilder – PGI vs. A-inntekt

Selv om begge kildene omhandler brukers inntekt, er de svært ulike og **ikke direkte sammenlignbare**.

**PGI (Sigrun)** gir aggregerte årsbeløp per inntektstype – kun pensjonsgivende inntekt etter skatteloven. Eneste juridisk korrekte grunnlag for beregningen.

**A-ordningen** gir detaljerte enkeltutbetalinger med dato, arbeidsgiver og metadata. Dekker et bredere inntektsbegrep inkl. ikke-pensjonsgivende poster. Kun visning for saksbehandler.

## Forbehandling og revurdering

Når en revurdering opprettes, kopieres den ferdigstilte forbehandlingen (`kopiertFra != null`). Kopien er datakontainer for revurderingen, har ingen oppgave, og ferdigstilles automatisk ved iverksettelse. `sisteFerdigstilteForbehandling` er ankerpunkt for å opprette revurderingen.

Hvis saksbehandler registrerer at brukers svar gir **endring til ugunst** (`endringErTilUgunstForBruker = JA`): revurderingen avsluttes og en ny forbehandling opprettes fra bunnen – bruker må få nytt forhåndsvarsel med oppdaterte tall (lovkrav).

Avbrutt forbehandling tilbakestiller etteroppgjøret til `MOTTATT_SKATTEOPPGJOER`. Kan ikke gjenopptas – saksbehandler må opprette ny.

## Spesialtilfeller

**Dødsfall i inntektsåret** – ingen etteroppgjør, ferdigstilles uten brev.

**Dødsfall etter inntektsåret** – etteroppgjøret gjennomføres normalt, men tilbakekreving overstyres til `INGEN_ENDRING` (uhensiktsmessig å kreve fra dødsbo, NAV-policy). Etterbetaling utbetales til dødsboet.

**Manglende skatteoppgjør** – saker uten PGI innen 1. desember → `MANGLER_SKATTEOPPGJOER`, manuell oppgave. Forbehandlingen markeres med `mottattSkatteoppgjoer = false`.

**Svarfrist** – produksjon bruker 1 måned (`EN_MND`), som er 3 uker lovfestet + postgang. `ETT_MINUTT`/`FEM_MINUTT` er kun for test.

## Nøkkelklasser

- `EtteroppgjoerService` – oppretter og oppdaterer etteroppgjør, koordinerer statusoverganger
- `EtteroppgjoerForbehandlingService` – oppretter, beregner og ferdigstiller forbehandlinger
- `EtteroppgjoerRevurderingService` – oppretter revurdering basert på ferdigstilt forbehandling
- `PensjonsgivendeInntektService` – henter PGI fra Sigrun
- `InntektskomponentService` – henter A-inntekt fra A-ordningen

## Avhengigheter

Kaller: `etterlatte-behandling` (sak, tilgang, grunnlag), `etterlatte-beregning` (avkortingsdata, beregningsresultat, terskler – se [beregning.md](beregning.md)), Sigrun (PGI), Inntektskomponenten (A-inntekt)
Lytter på: Skatteoppgjørhendelser via polling mot Sigrun (`LesSkatteoppgjoerHendelserJobService`, feature-togglet, cursor-basert)
