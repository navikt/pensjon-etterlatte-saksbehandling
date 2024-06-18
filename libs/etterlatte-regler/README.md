# etterlatte-regler

etterlatte-regler er et rammeverk for å skrive og kjøre regler. Rammeverket skal oppfylle etterlevelseskravene som 
stilles for å gjøre automatiske regelbeslutninger i NAV. 

## Etterlevelseskrav
Følgende etterlevelseskrav er vurdert ved utarbeidelse av rammeverket:
- K115.1 Behandling som benytter automatisering oppfyller vilkårene for dette
- K144.2 Det rettslige innholdet i løsningen skal være beskrevet og dokumentert på en allment forståelig måte
- K150.1 NAV må kunne gi partene i saken innsyn i sakens opplysninger
- K183.1 Bruker får prøvd sin sak etter riktig regelverk
- K187.1 Bruker skal ha tilstrekkelig informasjon om behandlingen når NAV fatter automatiske avgjørelser

Utfyllende informasjon om hvert krav finnes [her](https://etterlevelse.intern.nav.no/) (Intern side).

## Funksjonalitet

### Rammeverk
Rammeverket er skrevet som et bibliotek som kan importeres i tjenestene hvor det er behov for å skrive regler og kjøre
disse for å understøtte en automatisk vurdering. Rammeverket har støtte for versjonering slik at man til enhver tid 
kan se på regelresultatet hvilke versjon av rammeverket som er benyttet ved eksekvering av regel / regelflyt.

#### Eksempel på definisjon og kjøring av en regel
```kotlin
val beregnBarnepensjonRegel = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Velger hvilke regelverk som skal anvendes for beregning av barnepensjon",
    regelReferanse = toDoRegelReferanse
) velgNyesteGyldige (beregnBarnepensjon1967Regel og beregnBarnepensjon2024Regel)

val grunnlag = BarnepensjonGrunnlag(
    grunnbeloep = FaktumNode(kilde = saksbehandler, beskrivelse = "Grunnbeløp", verdi = BigDecimal(100_550)),
    antallSoeskenIKullet = FaktumNode(kilde = saksbehandler, beskrivelse = "Antall søsken i kullet", verdi = 2),
    avdoedForelder = FaktumNode(
        kilde = saksbehandler, 
        beskrivelse = "Avdød forelders trygdetid", 
        verdi = AvdoedForelder(trygdetid = BigDecimal(30))
    )
)
val periode = RegelPeriode(LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1))
val resultat = beregnBarnepensjonRegel.eksekver(grunnlag, periode)
```

### Periodisering
Når en regel kjøres, kreves det et grunnlag, samt en periode hvor regelen skal anvendes. Rammeverket vil først sjekke om
det finnes knekkpunkter i regelflyten (feks regelverksendringer eller grunnbeløp-reguleringer). Dette vil danne nye 
perioder i resultatet. Det betyr at om man sender inn en periode, kan resultatet likevel returnere et likt eller større
antall perioder.

### Regelflyt
Vanligvis vil det være behov for å definere flere ulike regler med avhengigheter til hverandre. Rammeverket
har støtte for å sette sammen regler i en regelflyt ved bruk av ulike regeltyper. Eksempler på ulike typer
regler er `benytter`, `definerKonstant`, `finnFaktumIGrunnlag`, `velgNyesteGyldige` osv. Her er det god
fleksibilitet for å definere flere typer ved behov for dette. 

### Regel
Rammeverket legger opp til at regler er enkle å implementere, samt at de er enkle å lese for og forstå hva regelen gjør.
Det er derfor laget en enkel dsl for å understøtte dette.

For å beskrive en regel, må følgende parametere settes:
- Gyldig fra dato
- Tekstlig beskrivelse av regel
- Referanse til regelspesifikasjon og versjon (Referansen vil typisk være en id som er angitt i regelspesifikasjonen på confluence)

#### Eksempel på regel for å finne trygdetidsfaktor
```kotlin
val trygdetidsFaktor = RegelMeta(
    gjelderFra = BP_1967_DATO,
    beskrivelse = "Finn trygdetidsfaktor",
    regelReferanse = RegelReferanse(id = "BP-BEREGNING-1967-TRYGDETIDSFAKTOR")
) benytter maksTrygdetid og trygdetidRegel med { maksTrygdetid, trygdetid ->
    minOf(trygdetid, maksTrygdetid) / maksTrygdetid
}
```


### Resultat / Subsumsjon
Resultattreet som produseres når en regel eksekveres dokumenterer følgende som et json-dokument:
- Hvilke regler som er kjørt og metadataene fra disse
- Faktum og konstanter som er brukt
- Tidspunkt regler er kjørt
- Versjon av rammeverket som er brukt ved kjøring

## Versjonering
Vi versjonerer regler som et løpende tall som starter med 1 eller 1.0

### Major versjon
Første siffer står for en regelspesifisering. <br>
Hver gang en spesifisering endres og regel i kode blir implementert etter ny spesifisering bumpes første siffer.

### Minor versjon
Andre siffer står for kodeendringer uten endring i regelspesifisering. <br>
For eksempel hvis det er kodefeil/bug i implementasjonen av en regel.

## Videre arbeid
