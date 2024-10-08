package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.ForskjelligAvdoedPeriode
import no.nav.etterlatte.libs.common.feilhaandtering.InternfeilException
import no.nav.etterlatte.libs.common.logging.sikkerlogger

class BeregningForskjelligeAvdoedeFoersteOgSistePeriode(
    message: String,
) : InternfeilException(detail = message)

class BeregningForskjelligeAvdoedeFoersteOgSistePeriodeMenAvdoedFantesIkkeISistePeriode(
    message: String,
) : InternfeilException(detail = message)

class BeregningFoerstePeriodeManglerAvdoedNummerto(
    message: String,
) : InternfeilException(detail = message)

fun finnEventuellForskjelligAvdoedPeriode(
    avdoede: List<Avdoed>,
    utbetalingsinfo: Utbetalingsinfo,
): ForskjelligAvdoedPeriode? {
    val foerstePeriode = utbetalingsinfo.beregningsperioder.minBy { it.datoFOM }
    val sistePeriode = utbetalingsinfo.beregningsperioder.maxBy { it.datoFOM }
    val foerstePeriodeAvdoede =
        if (foerstePeriode.datoFOM < BarnepensjonInnvilgelse.tidspunktNyttRegelverk) {
            listOfNotNull(foerstePeriode.trygdetidForIdent)
        } else {
            foerstePeriode.avdoedeForeldre
        }
    if (foerstePeriodeAvdoede?.toSet() == sistePeriode.avdoedeForeldre?.toSet()) {
        return null
    }
    val foersteAvdoedIdent = foerstePeriodeAvdoede?.singleOrNull()
    val foersteAvdoed = avdoede.find { it.fnr.value == foersteAvdoedIdent }

    foersteAvdoed ?: run {
        sikkerlogger().error(
            "Har en beregning som bruker forskjellige avdøde i første og siste periode, men unik avdød i " +
                "første periode kunne ikke finnes i listen over avdøde. Avdøde i første beregningperiode:" +
                " ${foerstePeriode.avdoedeForeldre}, avdøde i siste beregningsperiode: " +
                "${sistePeriode.avdoedeForeldre}, avdøde vi kjenner til: $avdoede.",
        )
        throw BeregningForskjelligeAvdoedeFoersteOgSistePeriode(
            "Har en beregning som bruker forskjellige avdøde i første og siste periode, men unik avdød i første periode" +
                " kunne ikke finnes i listen over avdøde.",
        )
    }

    val sisteAvdoedIdent = sistePeriode.avdoedeForeldre?.singleOrNull { it != foersteAvdoedIdent }
    val sisteAvdoed = avdoede.find { it.fnr.value == sisteAvdoedIdent }

    sisteAvdoed ?: run {
        sikkerlogger().error(
            "Har en beregning som bruker forskjellige avdøde i første og siste periode, men avdød i " +
                "siste periode kunne ikke finnes i listen over avdøde. Avdøde i første beregningperiode:" +
                " ${foerstePeriode.avdoedeForeldre}, avdøde i siste beregningsperiode: " +
                "${sistePeriode.avdoedeForeldre}, avdøde vi kjenner til: $avdoede.",
        )
        throw BeregningForskjelligeAvdoedeFoersteOgSistePeriodeMenAvdoedFantesIkkeISistePeriode(
            "Har en beregning som bruker forskjellige avdøde i første og siste periode, men avdød i siste periode" +
                " som var den ene forskjellige fra avdøde i første periode kunne ikke finnes i listen over " +
                "avdøde. Hvis det er flere enn to avdøde i beregningen så støtter vi ikke brev for det. ",
        )
    }

    val foerstePeriodeMedSisteAvdoed =
        utbetalingsinfo.beregningsperioder
            .filter { it.avdoedeForeldre?.contains(sisteAvdoedIdent) ?: false }
            .minByOrNull { it.datoFOM }

    foerstePeriodeMedSisteAvdoed ?: run {
        throw BeregningFoerstePeriodeManglerAvdoedNummerto(
            "Kunne ikke finne første periode der avdoed nr.2 ble brukt i beregningen. Dette skal ikke kunne skje " +
                "siden vi allerede har identifisert en periode der avdød nr. 2 er brukt.",
        )
    }

    return ForskjelligAvdoedPeriode(
        foersteAvdoed = foersteAvdoed,
        senereAvdoed = sisteAvdoed,
        senereVirkningsdato = foerstePeriodeMedSisteAvdoed.datoFOM,
    )
}
