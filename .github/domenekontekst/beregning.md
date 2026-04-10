# etterlatte-beregning

Beregner pensjonsytelser for BP og OMS basert på trygdetid, grunnbeløp og eventuell inntekt.

## Ansvarsområder

- Beregne ytelsesbeløp per periode for BP og OMS
- Avkorting: redusere OMS-ytelsen basert på forventet inntekt
- Etteroppgjør: etterskuddsvis justering av avkorting etter faktisk inntekt
- Støtte for manuell overstyring av beregning (med begrunnelse)

## Sentrale begreper

| Begrep | Forklaring |
|---|---|
| `Beregning` | Resultat av en beregningskjøring for én behandling |
| `Beregningsperiode` | Tidsperiode med beregnet beløp og grunnlag |
| `Avkorting` | Reduksjon av OMS-ytelse pga. inntekt |
| `OverstyrBeregning` | Manuell overstyring av beregnet beløp |
| `Grunnbeløp` | Folketrygdens grunnbeløp (G), brukes som beregningsgrunnlag |
| `EtteroppgjoerGrense` | Terskel for om differansen mellom utbetalt og faktisk skyldig stønad er stor nok til å gi et handlingsutfall (etterbetaling/tilbakekreving/ingen endring). Påvirker *utfallet*, ikke om vi trigger revurdering. |
| `BeregnetEtteroppgjoerResultat` | Beregnet differanse mellom hva bruker faktisk fikk utbetalt og hva de skulle hatt basert på faktisk inntekt. Produseres i forbehandlingen, videreføres til revurdering. |

## Etteroppgjør – beregningsperspektiv

Etteroppgjør er en to-trinns prosess der beregning deltar i begge:

**Trinn 1 – Forbehandling (preview)**
- `EtteroppgjoerService` (i `etterlatte-beregning`) kaller `beregnAvkortingForbehandling` med faktisk inntekt
- Beregner avkorting basert på faktisk inntekt (fra a-ordningen og pensjonsgivende inntekt fra Skatteetaten) – kildene er den dataen vi har på nåværende tidspunkt.
- Resultat: `BeregnetEtteroppgjoerResultat` med differanse, grense og `EtteroppgjoerResultatType`
- Forbehandlingen er et *preview* av det endelige vedtaket – ikke et eget vedtak

**Trinn 2 – Revurdering**
- Kopierer forbehandlingen (`kopiertFra`-referanse)
- Saksbehandler kan korrigere faktisk inntekt etter brukers tilbakemelding
- Normal behandlingsflyt (fatte → attestere → iverksette)

## Nøkkelklasser

- `BeregningService` – orkestrator; delegerer til BP- og OMS-spesifikke tjenester
- `BeregnBarnepensjonService` / `BeregnOmstillingsstoenadService` – ytelsesberegning
- `AvkortingService` – inntektsbasert avkorting og etteroppgjør
- `BeregningRepository` – persistering til Postgres

## Avhengigheter

Kaller: `etterlatte-behandling` (behandlingskontekst, tilgangskontroll og grunnlag), `etterlatte-trygdetid` (trygdetidsgrunnlag), vilkårsvurdering (via behandling)

## Regelendringer – prosess og versjonering

Beregningsreglene er definert med `RegelMeta` og `RegelReferanse` (inkl. `id` og valgfri `versjon`).

**Hver gang en beregningsregel endres gjelder:**
1. **Bump versjon** i `RegelReferanse` for alle regler som endres, f.eks.:
   ```kotlin
   regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-INSTITUSJONSOPPHOLD-SATS", versjon = "2.0")
   ```
2. **Gå igjennom regelendringen med fagressurs** i teamet før merge.
3. **Oppdater Confluence** med det nye regeltreet etter merge/deploy – beskriv hva som er endret og referér til ny versjon.

Dette er et krav, ikke en anbefaling – regeltreet brukes til sporbarhet og revisjon av beregninger.
