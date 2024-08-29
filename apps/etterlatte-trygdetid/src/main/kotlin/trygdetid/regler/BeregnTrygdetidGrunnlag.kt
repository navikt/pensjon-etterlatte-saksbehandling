package no.nav.etterlatte.trygdetid.regler

import no.nav.etterlatte.libs.regler.Regel
import no.nav.etterlatte.libs.regler.RegelMeta
import no.nav.etterlatte.libs.regler.RegelReferanse
import no.nav.etterlatte.libs.regler.benytter
import no.nav.etterlatte.libs.regler.finnFaktumIGrunnlag
import no.nav.etterlatte.libs.regler.med
import java.time.Month
import java.time.MonthDay
import java.time.Period

val periode: Regel<TrygdetidPeriodeGrunnlag, TrygdetidPeriodeMedPoengaar> =
    finnFaktumIGrunnlag(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Finner trygdetidsperiode fra grunnlag",
        finnFaktum = TrygdetidPeriodeGrunnlag::periode,
        finnFelt = { it },
    )

val beregnTrygdetidForPeriode =
    RegelMeta(
        gjelderFra = TRYGDETID_DATO,
        beskrivelse = "Beregner trygdetid fra og med periodeFra til og med periodeTil i år, måneder og dager",
        regelReferanse = RegelReferanse(id = "REGEL-TRYGDETID-BEREGNE-PERIODE"),
    ) benytter periode med { periode ->
        fun TrygdetidPeriodeMedPoengaar.erEttPoengaar() = fra.year == til.year && (poengInnAar || poengUtAar)

        fun TrygdetidPeriodeMedPoengaar.poengJustertFra() =
            if (poengInnAar) {
                fra.with(MonthDay.of(Month.JANUARY, 1))
            } else {
                fra
            }

        fun TrygdetidPeriodeMedPoengaar.poengJustertTil() =
            if (poengUtAar) {
                til.with(MonthDay.of(Month.DECEMBER, 31))
            } else {
                til
            }

        if (periode.erEttPoengaar()) {
            Period.ofYears(1)
        } else {
            Period.between(periode.poengJustertFra(), periode.poengJustertTil().plusDays(1))
        }
    }
