# Plan: G-sats for institusjonsopphold (BP og OMS)

## Problem
Saksbehandler kan i dag bare angi en heltalls-reduksjonsprosent (0–100 %) via `JA_EIGEN_PROSENT_AV_G`
for egendefinert institusjonsopphold. For OMS skal det nye feltet støtte G-multiplikator 0.4–2.25.
For å unngå to overlappende enum-verdier og to overlappende felt migreres løsningen til én felles
modell for BP og OMS.

## Tilnærming
- Erstatt `JA_EIGEN_PROSENT_AV_G` med `JA_TILPASSET_SATS` (brukes av både BP og OMS)
- Erstatt `egenReduksjon: Int` med `egenSatsG: String` (vilkårlig presisjon via `BigDecimal`)
- DB-migrasjon (Flyway) konverterer eksisterende data: `(100 - egenReduksjon) / 100.0`
- BP og OMS-regler oppdateres til å bruke G-faktor direkte (ikke Prosent-brøk)
- Frontend validerer ulikt per sakType: BP 0.0–1.0, OMS 0.4–2.25

---

## Todos

### 1. DB-migrasjon – `apps/etterlatte-beregning`
**Ny fil:** `src/main/resources/db/migration/V70__migrer_egen_prosent_til_tilpasset_sats.sql`

```sql
UPDATE beregningsgrunnlag
SET institusjonsopphold = (
    SELECT jsonb_agg(
        CASE
            WHEN elem->>'reduksjon' = 'JA_EIGEN_PROSENT_AV_G' THEN
                jsonb_build_object(
                    'reduksjon', 'JA_TILPASSET_SATS',
                    'egenSatsG', (
                        (100 - (elem->>'egenReduksjon')::int)::numeric / 100
                    )::text,
                    'begrunnelse', elem->'begrunnelse'
                )
            ELSE elem
        END
    )
    FROM jsonb_array_elements(institusjonsopphold) elem
)
WHERE institusjonsopphold IS NOT NULL
  AND institusjonsopphold::text LIKE '%JA_EIGEN_PROSENT_AV_G%';
```

- Migrer tilsvarende i `bp_beregningsgrunnlag` hvis `JA_EIGEN_PROSENT_AV_G` finnes der også
- Legg til integrasjonstest som verifiserer at gammel data leses korrekt etter migrasjon

### 2. Model – `libs/etterlatte-beregning-model`
**Fil:** `InstitusjonsoppholdBeregningsgrunnlag.kt`

- Erstatt `JA_EIGEN_PROSENT_AV_G(null)` med `JA_TILPASSET_SATS(null)` i `Reduksjon`-enum
- Erstatt `egenReduksjon: Int? = null` med `egenSatsG: String? = null`
- Legg til metode `gFaktor(): Beregningstall`:
  ```kotlin
  fun gFaktor(): Beregningstall = when {
      reduksjon == Reduksjon.JA_TILPASSET_SATS ->
          Beregningstall(BigDecimal(egenSatsG!!))
      else ->
          Beregningstall.somBroek(reduksjon.gjenvaerendeEtterReduksjon())
  }
  ```
- Fjern `prosentEtterReduksjon()` og `erEgendefinert()` når de ikke lenger er i bruk
- Oppdater tester i `InstitusjonsoppholdBeregningsgrunnlagTest.kt`

### 3. Beregningsregel OMS – `apps/etterlatte-beregning`
**Fil:** `InstitusjonsoppholdreglerOMS.kt`

- Erstatt `institusjonsoppholdRegelOMS: Regel<…, Prosent>` med:
  ```kotlin
  val institusjonsoppholdGFaktorRegelOMS: Regel<OmstillingstoenadGrunnlag, Beregningstall> =
      finnFaktumIGrunnlag(…) { it?.gFaktor() ?: Beregningstall(1) }
  ```
- Oppdater `institusjonsoppholdSatsRegelOMS`:
  ```kotlin
  benytter grunnbeloep og institusjonsoppholdGFaktorRegelOMS med { grunnbeloep, gFaktor ->
      gFaktor.multiply(grunnbeloep.grunnbeloepPerMaaned)
  }
  ```
- Oppdater tester i `BeregnOmstillingsstoenadServiceTest.kt`

### 4. Beregningsregel BP – `apps/etterlatte-beregning`
**Fil:** `InstitusjonsoppholdreglerBP.kt`

- Tilsvarende som OMS: erstatt `Prosent`-basert regel med `Beregningstall`-basert via `gFaktor()`
- Oppdater tester i `BeregnBarnepensjonServiceTest.kt` og `InstitusjonsoppholdTest.kt`

### 5. Regelversjonering og Confluence-dokumentasjon
> ⚠️ **Må gjøres av utvikleren som merger og deployer, etter gjennomgang med fagressurs i teamet.**

- Oppdater `versjon`-feltet i `RegelReferanse` for alle regler som endres, f.eks.:
  ```kotlin
  regelReferanse = RegelReferanse(id = "OMS-BEREGNING-2024-INSTITUSJONSOPPHOLD-SATS", versjon = "2.0")
  ```
- Dokumenter det oppdaterte regeltreet i Confluence:
  - Hvilke regler som er endret
  - Hva som er nytt (G-multiplikator i stedet for Prosent-brøk)
  - Ny `versjon` i `RegelReferanse`
- Gjennomgå regelendringen med fagressurs i teamet **før** merge

### 6. Frontend – typer `Beregning.ts`
**Fil:** `client/src/shared/types/Beregning.ts`

- Erstatt `'JA_EIGEN_PROSENT_AV_G'` med `'JA_TILPASSET_SATS'` i `ReduksjonKeyBP` og `ReduksjonKeyOMS`
- Erstatt `egenReduksjon?: string | null` med `egenSatsG?: string | null` i `InstitusjonsoppholdIBeregning`
- Oppdater labels i `ReduksjonBP` og `ReduksjonOMS`, f.eks.:
  - `JA_TILPASSET_SATS: 'Ja, tilpasset sats (G)'`

### 7. Frontend – skjema `InstitusjonsoppholdBeregningsgrunnlagSkjema.tsx`
**Fil:** `…/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagSkjema.tsx`

- Vis `TextField` for `data.egenSatsG` når `reduksjon === 'JA_TILPASSET_SATS'`
- Validering per sakType:
  - BP: `0.0 ≤ parseFloat(v) ≤ 1.0`
  - OMS: `0.4 ≤ parseFloat(v) ≤ 2.25`
- Oppdater label og hjelpetekst
- Fjern gammelt `egenReduksjon`-felt

### 8. Frontend – tabell `InstitusjonsoppholdBeregningsgrunnlagTable.tsx`
**Fil:** `…/institusjonsopphold/InstitusjonsoppholdBeregningsgrunnlagTable.tsx`

- Oppdater "Egen reduksjon"-kolonnen til å vise `egenSatsG + " G"` (eller `"-"`)
- Kolonneoverskrift kan med fordel endres til "G-sats"

---

## Notater

### Presisjon
`egenSatsG` lagres som `String` i JSONB. Konverter med `BigDecimal(egenSatsG)` (ikke `toDouble()`)
for å beholde vilkårlig presisjon.

### Migrasjonsformel
`egenReduksjon = 40` → `egenSatsG = "0.60"` (dvs. `(100 - 40) / 100 = 0.60`)

### Ingen backward-compat nødvendig i kode
Etter DB-migrasjonen vil ingen rader lenger ha `JA_EIGEN_PROSENT_AV_G` eller `egenReduksjon`.
`gFaktor()` trenger derfor ingen fallback-gren for gammelt format.

### BP-regler endres, men BP-logikk er uendret
Preset-verdiene (`JA_VANLIG`, `JA_FORELDRELOES`) bruker fortsatt `gjenvaerendeEtterReduksjon()`
via `gFaktor()`. Kun den egendefinerte stien endres fra Prosent til BigDecimal.

