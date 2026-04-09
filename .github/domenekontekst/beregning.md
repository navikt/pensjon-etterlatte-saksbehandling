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

## Nøkkelklasser

- `BeregningService` – orkestrator; delegerer til BP- og OMS-spesifikke tjenester
- `BeregnBarnepensjonService` / `BeregnOmstillingsstoenadService` – ytelsesberegning
- `AvkortingService` – inntektsbasert avkorting og etteroppgjør
- `BeregningRepository` – persistering til Postgres

## Avhengigheter

Kaller: `etterlatte-behandling` (behandlingskontekst, tilgangskontroll og grunnlag), `etterlatte-trygdetid` (trygdetidsgrunnlag), vilkårsvurdering (via behandling)
