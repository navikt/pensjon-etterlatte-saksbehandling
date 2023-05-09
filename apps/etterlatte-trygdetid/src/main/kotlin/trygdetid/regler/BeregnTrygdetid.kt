package no.nav.etterlatte.trygdetid.regler

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

// TODO dato settes riktig senere
val TRYGDETID_DATO = LocalDate.of(1900, 1, 1)

data class TrygdetidPeriodeGrunnlag(
    val periodeFra: FaktumNode<LocalDate>,
    val periodeTil: FaktumNode<LocalDate>
)

data class TotalTrygdetidGrunnlag(
    val beregnetTrygdetidPerioder: FaktumNode<List<Period>>
)

val periodeFra: Regel<TrygdetidPeriodeGrunnlag, LocalDate> = finnFaktumIGrunnlag(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "Periode fra",
    finnFaktum = TrygdetidPeriodeGrunnlag::periodeFra,
    finnFelt = { it }
)

val periodeTil: Regel<TrygdetidPeriodeGrunnlag, LocalDate> = finnFaktumIGrunnlag(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "Periode til",
    finnFaktum = TrygdetidPeriodeGrunnlag::periodeTil,
    finnFelt = { it }
)

val beregnTrygdetidForPeriode = RegelMeta(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "Beregner trygdetid fra og med periodeFra til og med periodeTil i år, måneder og dager",
    regelReferanse = RegelReferanse(id = "REGEL-TRYGDETID-BEREGNE-PERIODE")
) benytter periodeFra og periodeTil med { periodeFra, periodeTil ->
    Period.between(periodeFra, periodeTil.plusDays(1))
}

val beregnetTrygdetidPerioder: Regel<TotalTrygdetidGrunnlag, List<Period>> = finnFaktumIGrunnlag(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "Beregnet trygdetidsperioder",
    finnFaktum = TotalTrygdetidGrunnlag::beregnetTrygdetidPerioder,
    finnFelt = { it }
)

val maksTrygdetid = definerKonstant<TotalTrygdetidGrunnlag, Int>(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = RegelReferanse("REGEL-TOTAL-TRYGDETID-MAKS-ANTALL-ÅR"),
    verdi = 40
)

val dagerPrMaanedTrygdetid = definerKonstant<TotalTrygdetidGrunnlag, Int>(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "En måned trygdetid tilsvarer 30 dager",
    regelReferanse = RegelReferanse("REGEL-TOTAL-TRYGDETID-DAGER-PR-MND-TRYGDETID"),
    verdi = 30
)

val totalTrygdetidFraPerioder = RegelMeta(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "Beregner trygdetid fra perioder",
    regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID-SLÅ-SAMMEN-PERIODER")
) benytter beregnetTrygdetidPerioder og dagerPrMaanedTrygdetid med { trygdetidPerioder, antallDagerEnMaanedTrygdetid ->
    trygdetidPerioder
        .reduce { acc, period -> acc.plus(period) }
        .let {
            val dagerResterende = it.days.mod(antallDagerEnMaanedTrygdetid)
            val maanederOppjustert = it.months + (it.days - dagerResterende).div(antallDagerEnMaanedTrygdetid)
            Period.of(it.years, maanederOppjustert, dagerResterende).normalized()
        }
}

val totalTrygdetidAvrundet = RegelMeta(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "Avrunder trygdetid til nærmeste hele år basert på måneder",
    regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID-AVRUNDING")
) benytter totalTrygdetidFraPerioder med { totalTrygdetidFraPerioder ->
    if (totalTrygdetidFraPerioder.months >= 6) {
        totalTrygdetidFraPerioder.years + 1
    } else {
        totalTrygdetidFraPerioder.years
    }
}

val beregnAntallAarTotalTrygdetid = RegelMeta(
    gjelderFra = TRYGDETID_DATO,
    beskrivelse = "Beregner antall år trygdetid totalt",
    regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID")
) benytter totalTrygdetidAvrundet og maksTrygdetid med { totalTrygdetidAvrundet, maksTrygdetid ->
    minOf(totalTrygdetidAvrundet, maksTrygdetid)
}