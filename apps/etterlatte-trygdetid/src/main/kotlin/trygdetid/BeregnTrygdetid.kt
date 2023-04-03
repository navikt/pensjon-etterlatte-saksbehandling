package no.nav.etterlatte.trygdetid

import no.nav.etterlatte.libs.regler.FaktumNode
import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.definerKonstant
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import no.nav.etterlatte.libs.regler.og
import java.time.LocalDate
import java.time.Period

val GYLDIG_FRA = LocalDate.of(1900, 1, 1)

data class TrygdetidPeriodeGrunnlag(
    val periodeFra: FaktumNode<LocalDate>,
    val periodeTil: FaktumNode<LocalDate>
)

data class TotalTrygdetidGrunnlag(
    val beregnetTrygdetidPerioder: FaktumNode<List<Period>>
)

val periodeFra: Regel<TrygdetidPeriodeGrunnlag, LocalDate> = finnFaktumIGrunnlag(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Periode fra",
    finnFaktum = TrygdetidPeriodeGrunnlag::periodeFra,
    finnFelt = { it }
)

val periodeTil: Regel<TrygdetidPeriodeGrunnlag, LocalDate> = finnFaktumIGrunnlag(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Periode til",
    finnFaktum = TrygdetidPeriodeGrunnlag::periodeTil,
    finnFelt = { it }
)

val beregnTrygdetidMellomToDatoer = RegelMeta(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Beregner trygdetid mellom to datoer i år, måneder og dager",
    regelReferanse = RegelReferanse(id = "TODO")
) benytter periodeFra og periodeTil med { periodeFra, periodeTil ->
    Period.between(periodeFra, periodeTil.plusDays(1))
}

val beregnetTrygdetidPerioder: Regel<TotalTrygdetidGrunnlag, List<Period>> = finnFaktumIGrunnlag(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Beregnet trygdetidsperioder",
    finnFaktum = TotalTrygdetidGrunnlag::beregnetTrygdetidPerioder,
    finnFelt = { it }
)

val maksTrygdetid = definerKonstant<TotalTrygdetidGrunnlag, Int>(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = RegelReferanse("TODO"),
    verdi = 40
)

val beregnAntallAarTrygdetid = RegelMeta(
    gjelderFra = GYLDIG_FRA,
    beskrivelse = "Beregner antall år trygdetid totalt",
    regelReferanse = RegelReferanse(id = "TODO")
) benytter beregnetTrygdetidPerioder og maksTrygdetid med { beregnetTrygdetidPerioder, maksTrygdetid ->
    // TODO denne må regelspesifiseres
    val totalTrygdetid = beregnetTrygdetidPerioder.reduce { acc, period -> acc.plus(period) }.normalized()
    val avrundOpp = totalTrygdetid.months > 0 || totalTrygdetid.days > 0
    if (avrundOpp) {
        minOf(totalTrygdetid.years + 1, maksTrygdetid)
    } else {
        minOf(totalTrygdetid.years, maksTrygdetid)
    }
}