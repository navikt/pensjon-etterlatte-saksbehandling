# Etteroppgjør (OMS)

Årlig etterskuddsvis kontroll av OMS-utbetalinger mot faktisk inntekt fra skatteoppgjøret. Kun OMS – ikke BP.

## Ansvarsområder

- Motta skatteoppgjørhendelser fra Sigrun og opprette etteroppgjør per sak
- Forbehandling: hente PGI/A-inntekt, beregne avvik, sende forhåndsvarsel
- Opprette revurdering ved tilbakekreving/etterbetaling
- Håndtere svarfrist, brukers tilbakemelding og omgjøring

## Sentrale begreper

| Begrep | Forklaring |
|---|---|
| `inntektsaar` | Året vi gjør opp (f.eks. 2024). Etteroppgjøret behandles påfølgende år. |
| `Etteroppgjoer` | Statussporingsobjekt – ett per sak per inntektsår. |
| `EtteroppgjoerForbehandling` | Arbeidsobjektet der saksbehandler behandler etteroppgjøret. |
| Forhåndsvarsel | Brev til bruker med beregnet avvik. Svarfrist 1 måned. |
| PGI | Pensjonsgivende inntekt fra Sigrun – eneste beregningsgrunnlag. |
| A-inntekt | Detaljert inntektsdata fra A-ordningen – kun visning for saksbehandler, brukes IKKE i beregning. |

## Nøkkelklasser

- `EtteroppgjoerService` – oppretter og oppdaterer etteroppgjør, koordinerer statusoverganger
- `EtteroppgjoerForbehandlingService` – oppretter, beregner og ferdigstiller forbehandlinger
- `EtteroppgjoerRevurderingService` – oppretter revurdering basert på ferdigstilt forbehandling
- `PensjonsgivendeInntektService` – henter PGI fra Sigrun (beregningsgrunnlag)
- `InntektskomponentService` – henter A-inntekt fra A-ordningen (visningsdata)

## Avhengigheter

Kaller: `etterlatte-behandling` (sak, tilgang, grunnlag), `etterlatte-beregning` (avkortingsdata, beregningsresultat), Sigrun (PGI), Inntektskomponenten (A-inntekt)
Lytter på: Skatteoppgjørhendelser via polling mot Sigrun

## Flyt

Skatteoppgjørhendelse mottas fra Sigrun (polling, feature-togglet) → `Etteroppgjoer` opprettes med status `VENTER_PAA_SKATTEOPPGJOER` → skatteoppgjør mottas → oppgave opprettes for saksbehandler → saksbehandler oppretter `EtteroppgjoerForbehandling` manuelt → PGI og A-inntekt hentes → beregning kjøres → forhåndsvarsel sendes → svarfrist 1 måned → resultat avgjør videre:

- **TILBAKEKREVING / ETTERBETALING** → revurdering opprettes → normal vedtaksflyt → `FERDIGSTILT`
- **INGEN_ENDRING** → `FERDIGSTILT` direkte (med eller uten brev avhengig av om utbetaling fantes)

## Statusoverganger – `EtteroppgjoerStatus`

| Fra | Til | Trigger |
|---|---|---|
| – | `VENTER_PAA_SKATTEOPPGJOER` | Etteroppgjør opprettet (ved iverksettelse) |
| `VENTER_PAA_SKATTEOPPGJOER` | `MOTTATT_SKATTEOPPGJOER` | Skatteoppgjørhendelse mottas |
| `VENTER_PAA_SKATTEOPPGJOER` | `MANGLER_SKATTEOPPGJOER` | Jobb etter 1. des, PGI mangler |
| `MOTTATT/MANGLER` | `UNDER_FORBEHANDLING` | Forbehandling opprettes |
| `UNDER_FORBEHANDLING` | `VENTER_PAA_SVAR` | Forhåndsvarsel sendt |
| `UNDER_FORBEHANDLING` | `FERDIGSTILT` | Ingen brev nødvendig (ingen utbetaling / dødsfall) |
| `VENTER_PAA_SVAR` | `UNDER_REVURDERING` | Revurdering opprettes |
| `VENTER_PAA_SVAR` | `FERDIGSTILT` | Ingen avvik |
| `UNDER_REVURDERING` | `FERDIGSTILT` | Revurdering iverksatt |
| `UNDER_REVURDERING/OMGJOERING` | `MOTTATT_SKATTEOPPGJOER` | Tilbakestilling (endring til ugunst) |
| `UNDER_REVURDERING/OMGJOERING` | `VENTER_PAA_SVAR` | Tilbakestilling (endring til gunst) |
| `FERDIGSTILT/VENTER_PAA_SVAR` | `OMGJOERING` | Klageomgjøring |

`OMGJOERING` er midlertidig – ny forbehandling opprettes i separat transaksjon for å unngå å blokkere pågående behandlinger.

## Forretningsregler og beslutninger

| Regel/Beslutning | Begrunnelse | Lovfestet? |
|---|---|---|
| Tilbakekrevingsterskel: differanse > 1 rettsgebyr | Lovkrav | Ja |
| Etterbetalingsterskel: differanse > ¼ rettsgebyr | Lavere terskel for å betale ut enn å kreve tilbake | Ja |
| Rettsgebyr beregnes per 31. des i inntektsåret | Konsistens ved gjentatte beregninger | Ja |
| Ingen tilbakekreving ved dødsfall → `INGEN_ENDRING` | Uhensiktsmessig å kreve fra dødsbo | Nei (NAV-policy) |
| Etterbetaling ved dødsfall → utbetales til dødsbo | Bruker har krav på pengene | Ja |
| Forbehandling opprettes IKKE automatisk | Ville blokkere pågående behandlinger i saken | Nei (designvalg) |
| Svarfrist 1 måned (3 uker + postgang) | Lovkrav + praktisk tilnærming | Ja |
| `endringErTilUgunstForBruker = JA` → avbryt revurdering, ny forbehandling | Må sende nytt forhåndsvarsel med oppdaterte tall | Ja |
| PGI er eneste beregningsgrunnlag, A-inntekt er kun visning | PGI = pensjonsgivende inntekt etter skatteloven, A-ordningen har bredere scope | Ja |

## Spesialtilfeller og farer

⚠️ **Dødsfall i inntektsåret** – `opphoerSkyldesDoedsfallIEtteroppgjoersaar == JA` → ingen etteroppgjør, ferdigstilles uten brev.

⚠️ **Dødsfall etter inntektsåret** – Etteroppgjøret gjennomføres normalt. Etterbetaling → dødsbo. Tilbakekreving → `INGEN_ENDRING`.

⚠️ **Manglende skatteoppgjør** – Saker uten PGI innen 1. des → `MANGLER_SKATTEOPPGJOER`, manuell oppgave. `mottattSkatteoppgjoer = false` på forbehandlingen.

⚠️ **PGI ≠ A-inntekt** – Tallene er ikke sammenlignbare. PGI har aggregerte årsbeløp kun for pensjonsgivende inntekt. A-ordningen har detaljerte utbetalinger inkl. ikke-pensjonsgivende inntekter.

⚠️ **Avbrutt forbehandling** – Etteroppgjøret tilbakestilles til `MOTTATT_SKATTEOPPGJOER`. Saksbehandler må opprette ny forbehandling fra bunnen. Kan ikke gjenopptas.

## Forbehandling vs. revurderingsforbehandling

Revurdering kopierer ferdigstilt forbehandling (`kopiertFra != null`). Kopien er datakontainer for revurderingen, har ingen oppgave, og ferdigstilles automatisk ved iverksettelse. `sisteFerdigstilteForbehandling` er ankerpunkt for å opprette revurdering.

## Oppgavehåndtering

| Oppgavetype | Beskrivelse |
|---|---|
| `ETTEROPPGJOER` | Én oppgave per forbehandling. Tom `referanse` = klar til opprettelse, deretter oppdateres til `forbehandlingId`. |
| `ETTEROPPGJOER_OPPRETT_REVURDERING` | Opprettes ved svarfrist-utløp. Saksbehandler oppretter revurderingen manuelt. |

## Teknisk kontekst

- `ETTEROPPGJOER_AAR = 2024` – tidligste inntektsår, ikke neste kjøring.
- Etteroppgjørsdata (faktisk inntekt, beregnet resultat) lagres i `etterlatte-beregning`, ikke `etterlatte-behandling`.
- Skatteoppgjørhendelser hentes ved polling (cursor-basert med sekvensnummer). Feature-togglet.
- `har*`-flagg og `EtteroppgjoerFilter` er fra gradvis utrulling 2024–2025. På vei ut.
- `harIngenInntekt`-flagget er kun sporbarhet – dokumenterer at regelen fikk null inntekt.
- `aktivitetspliktOverholdt`/`aktivitetspliktBegrunnelse` er internt notat, påvirker ikke flyten.
- `EtteroppgjoerSvarfrist.ETT_MINUTT`/`FEM_MINUTT` er kun for test/dev.
