package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

val GYLDIG_FRA = LocalDate.of(1900, 1, 1)

data class TrygdetidPeriodeGrunnlag(
    val periodeFra: FaktumNode<LocalDate>,
    val periodeTil: FaktumNode<LocalDate>
)

data class TrygdetidDelberegningerGrunnlag(
    val delBeregninger: FaktumNode<List<Int>>
)

val periodeFra: Regel<TrygdetidPeriodeGrunnlag, LocalDate> = finnFaktumIGrunnlag(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Periode fra",
    finnFaktum = TrygdetidPeriodeGrunnlag::periodeFra,
    finnFelt = { it }
)

val periodeTil: Regel<TrygdetidPeriodeGrunnlag, LocalDate> = finnFaktumIGrunnlag(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Periode fra",
    finnFaktum = TrygdetidPeriodeGrunnlag::periodeTil,
    finnFelt = { it }
)

val beregnAntallDagerTrygdetidMellomToDatoer = RegelMeta(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Beregner antall dager trygdetid mellom to datoer",
    regelReferanse = RegelReferanse(id = "TODO")
) benytter periodeFra og periodeTil med { periodeFra, periodeTil ->
    ChronoUnit.DAYS.between(periodeFra, periodeTil).toInt()
}

val delBeregninger: Regel<TrygdetidDelberegningerGrunnlag, List<Int>> = finnFaktumIGrunnlag(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Trygdetidsgrunnlag",
    finnFaktum = TrygdetidDelberegningerGrunnlag::delBeregninger,
    finnFelt = { it }
)

val beregnAntallAarTrygdetidFraDager = RegelMeta(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Beregner antall år trygdetid totalt",
    regelReferanse = RegelReferanse(id = "TODO")
) benytter delBeregninger med { delBeregninger ->
    val days = delBeregninger.sum()
    if (days > 0) {
        BigDecimal(days).divide(BigDecimal(365), 0, RoundingMode.UP).toInt()
    } else {
        days
    }
}