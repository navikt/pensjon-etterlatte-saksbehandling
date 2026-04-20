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
| `Avkorting` | Reduksjon av OMS-ytelse pga. inntekt (ftrl § 17-9) |
| `OverstyrBeregning` | Manuell overstyring av beregnet beløp |
| `Grunnbeløp` | Folketrygdens grunnbeløp (G), brukes som beregningsgrunnlag |
| `EtteroppgjoerGrense` | Terskel for om differansen mellom utbetalt og faktisk skyldig stønad er stor nok til å gi et handlingsutfall (etterbetaling/tilbakekreving/ingen endring). Påvirker *utfallet*, ikke om vi trigger revurdering. |
| `BeregnetEtteroppgjoerResultat` | Beregnet differanse mellom hva bruker faktisk fikk utbetalt og hva de skulle hatt basert på faktisk inntekt. Produseres i forbehandlingen, videreføres til revurdering. |

## Etteroppgjør – beregningsperspektiv (forskriften til kap. 17 § 9)

Etteroppgjør er en to-trinns prosess der beregning deltar i begge:

**Trinn 1 – Forbehandling (preview)**
- beregning mottar `EtteroppgjoerBeregnFaktiskInntektRequest` fra behandling-app
- `EtteroppgjoerService.beregnAvkortingForbehandling` → `Avkorting.beregnEtteroppgjoer`
- Oppretter en ny `Avkorting` for forbehandlingen med et `Etteroppgjoer`-objekt (basert på `AarsoppgjoerLoepende` fra siste iverksatte behandling) med `FaktiskInntekt`, lagret under `forbehandlingId` – originalen røres ikke
- Deretter beregnes `BeregnetEtteroppgjoerResultat` med differanse, grense og `EtteroppgjoerResultatType`

**Trinn 2 – Revurdering**
- Kopierer avkorting fra siste iverksatte behandling (`AvkortingService.kopierOgReberegnAvkorting`)
- `avkortingMedOppdatertAarsoppgjoerFraForbehandling` erstatter `AarsoppgjoerLoepende`-entryen med `Etteroppgjoer`-entryen fra forbehandlingens avkorting
- Beregner og lagrer ny avkorting for revurderingen

## Nøkkelklasser

- `BeregningService` – orkestrator; delegerer til BP- og OMS-spesifikke tjenester
- `BeregnBarnepensjonService` / `BeregnOmstillingsstoenadService` – ytelsesberegning
- `AvkortingService` – inntektsbasert avkorting og etteroppgjør
- `BeregningRepository` – persistering til Postgres

## Avkorting – domenekonsepter (OMS) (ftrl § 17-9)

### `Avkorting` og `Aarsoppgjoer`

`Avkorting.aarsoppgjoer: List<Aarsoppgjoer>` holder **komplett historikk** over alle år. Klassen `Aarsoppgjoer` er en sealed class med to varianter:

| Variant | Når | Inntekt |
|---|---|---|
| `AarsoppgjoerLoepende` | Løpende år med forventet inntekt | `inntektsavkorting: List<Inntektsavkorting>` der `Inntektsavkorting.grunnlag: ForventetInntekt` |
| `Etteroppgjoer` | Avsluttet år etter skatteoppgjøret | `inntekt: FaktiskInntekt` (faktisk inntekt fra Skatteetaten) |

`Avkorting.toDto(fraVirkningstidspunkt?)` filtrerer `avkortetYtelse` til år >= virk-år. `avkortingGrunnlag` returnerer alltid full historikk. Ved kall til `hentFullfoertAvkorting` (bruker behandlingens virk) vil `avkortetYtelse` for tidligere etteroppgjørsår **ikke** være med hvis virk er etter etteroppgjørsåret.

### Hva er "innvilgede måneder"?

Den korrekte måten å finne måneder der ytelse er beregnet > 0 er å sjekke `avkortetYtelse[*].ytelseFoerAvkorting > 0`:
- **Vedtaksvurderingens innvilget periode**: skiller *ikke* mellom sanksjon (ytelse = 0) og full ytelse — ikke egnet
- **`avkortetYtelse[*].ytelseEtterAvkorting`**: kan være 0 pga. inntektsavkorting — ikke egnet
- **`ytelseFoerAvkorting > 0`**: ytelsen *før* inntektsavkorting, men *etter* sanksjon — korrekt

### Sperrer mot inntektsendring i etteroppgjørsår

To separate sperrer blokkerer inntektsposteringer for år som har `Etteroppgjoer` – disse gjelder **kun den normale `ForventetInntekt`-flyten** (`oppdaterMedInntektsgrunnlag`), ikke etteroppgjørsflyten som bruker `beregnEtteroppgjoer` direkte:
1. `Avkorting.oppdaterMedInntektsgrunnlag` kaster `InternfeilException` dersom `hentEllerOpprettAarsoppgjoer(fom)` returnerer `Etteroppgjoer`
2. `AvkortingValider.validerInntekter` kaster `InntektForTidligereAar` dersom `nyeGrunnlag` inneholder et år som allerede er `Etteroppgjoer`

### Navnekollisjonen `Etteroppgjoer`

Det finnes to ulike konsepter med samme navn i to apper:
- **`avkorting.Etteroppgjoer`** (beregning-app): variant av `Aarsoppgjoer` som representerer et avsluttet år med `FaktiskInntekt`. Opprettes i `Avkorting.beregnEtteroppgjoer`.
- **`behandling.etteroppgjoer.Etteroppgjoer`** (behandling-app): domeneentitet som tracker status for hele etteroppgjørsprosessen for en sak/år (f.eks. `MOTTATT_SKATTEOPPGJOER`, `UNDER_REVURDERING`, `FERDIGSTILT`).

### `FaktiskInntekt` vs. `ForventetInntekt` (forskriften til kap. 17 § 10)

Inntektsendringer skal gjelde fremover og fordeles jevnt over gjenværende måneder i året – dette er bakgrunnen for at `ForventetInntekt` har `fratrekkInnAar`-felt for inntekt opptjent før virkningstidspunktet.

Begge extender `AvkortingGrunnlag`, men har ulik feltstruktur:
- `FaktiskInntekt`: separate kategorier (`loennsinntekt`, `naeringsinntekt`, `afp`, `utlandsinntekt`)
- `ForventetInntekt`: aggregerte felt (`inntektTom`, `inntektUtlandTom`, `fratrekkInnAar`, `fratrekkInnAarUtland`)

`Inntektsavkorting.grunnlag` er **konkret typet** som `ForventetInntekt` — ikke den abstrakte `AvkortingGrunnlag`. Konvertering mellom typene er ikke triviell.

### Kopiering og reberegning ved revurdering

`AvkortingService.kopierOgReberegnAvkorting` er inngangspunktet for revurderinger. Her er `forrigeAvkorting: Avkorting` tilgjengelig direkte (ingen ekstra API-kall). Metoden:
1. Kopierer aarsoppgjoer fra forrige behandling (inkl. eventuelle `Etteroppgjoer`-år)
2. Reberegner avkorting basert på ny beregning og sanksjoner

For revurderinger med årsak `ETTEROPPGJOER` brukes `avkortingMedOppdatertAarsoppgjoerFraForbehandling`: årsoppgjøret fra forbehandlingen (som har `Etteroppgjoer` med faktisk inntekt) erstatter det tilsvarende året i kopiert avkorting.

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
