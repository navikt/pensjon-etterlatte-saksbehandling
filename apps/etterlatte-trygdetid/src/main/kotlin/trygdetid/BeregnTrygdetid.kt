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

val TRYGDETID_OMS_DATO = LocalDate.of(1900, 1, 1)

data class TrygdetidPeriodeGrunnlag(
    val periodeFra: FaktumNode<LocalDate>,
    val periodeTil: FaktumNode<LocalDate>
)

data class TotalTrygdetidGrunnlag(
    val beregnetTrygdetidPerioder: FaktumNode<List<Period>>
)

val periodeFra: Regel<TrygdetidPeriodeGrunnlag, LocalDate> = finnFaktumIGrunnlag(
    gjelderFra = TRYGDETID_OMS_DATO,
    beskrivelse = "Periode fra",
    finnFaktum = TrygdetidPeriodeGrunnlag::periodeFra,
    finnFelt = { it }
)

val periodeTil: Regel<TrygdetidPeriodeGrunnlag, LocalDate> = finnFaktumIGrunnlag(
    gjelderFra = TRYGDETID_OMS_DATO,
    beskrivelse = "Periode til",
    finnFaktum = TrygdetidPeriodeGrunnlag::periodeTil,
    finnFelt = { it }
)

val beregnTrygdetidMellomToDatoer = RegelMeta(
    gjelderFra = TRYGDETID_OMS_DATO,
    beskrivelse = "Beregner trygdetid mellom to datoer i år, måneder og dager",
    regelReferanse = RegelReferanse(id = "REGEL-TRYGDETID-BEREGNE-PERIODE")
) benytter periodeFra og periodeTil med { periodeFra, periodeTil ->
    Period.between(periodeFra, periodeTil.plusDays(1))
}

val beregnetTrygdetidPerioder: Regel<TotalTrygdetidGrunnlag, List<Period>> = finnFaktumIGrunnlag(
    gjelderFra = TRYGDETID_OMS_DATO,
    beskrivelse = "Beregnet trygdetidsperioder",
    finnFaktum = TotalTrygdetidGrunnlag::beregnetTrygdetidPerioder,
    finnFelt = { it }
)

val maksTrygdetid = definerKonstant<TotalTrygdetidGrunnlag, Int>(
    gjelderFra = TRYGDETID_OMS_DATO,
    beskrivelse = "Full trygdetidsopptjening er 40 år",
    regelReferanse = RegelReferanse("REGEL-TOTAL-TRYGDETID-MAKS-ANTALL-ÅR"),
    verdi = 40
)

val totalTrygdetidFraPerioder = RegelMeta(
    gjelderFra = TRYGDETID_OMS_DATO,
    beskrivelse = "Beregner trygdetid fra perioder",
    regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID-SLÅ-SAMMEN-PERIODER")
) benytter beregnetTrygdetidPerioder med { beregnetTrygdetidPerioder ->
    beregnetTrygdetidPerioder.reduce { acc, period -> acc.plus(period) }.normalized()
}

val totalTrygdetidAvrundet = RegelMeta(
    gjelderFra = TRYGDETID_OMS_DATO,
    beskrivelse = "Runder av trygdetid opp til hele år",
    regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID-AVRUNDING")
) benytter totalTrygdetidFraPerioder med { totalTrygdetidFraPerioder ->
    if (totalTrygdetidFraPerioder.months > 0 || totalTrygdetidFraPerioder.days > 0) {
        totalTrygdetidFraPerioder.years + 1
    } else {
        totalTrygdetidFraPerioder.years
    }
}

val beregnAntallAarTrygdetid = RegelMeta(
    gjelderFra = TRYGDETID_OMS_DATO,
    beskrivelse = "Beregner antall år trygdetid totalt",
    regelReferanse = RegelReferanse(id = "REGEL-TOTAL-TRYGDETID")
) benytter totalTrygdetidAvrundet og maksTrygdetid med { totalTrygdetidAvrundet, maksTrygdetid ->
    minOf(totalTrygdetidAvrundet, maksTrygdetid)
}