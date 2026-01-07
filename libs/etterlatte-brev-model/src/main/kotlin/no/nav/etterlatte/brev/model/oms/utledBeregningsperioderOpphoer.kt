package no.nav.etterlatte.brev.model.oms

import no.nav.etterlatte.brev.model.OmstillingsstoenadBeregningsperiode
import no.nav.etterlatte.libs.common.behandling.DetaljertBehandling
import java.time.LocalDate

fun utledBeregningsperioderOpphoer(
    behandling: DetaljertBehandling,
    beregningsperioder: List<OmstillingsstoenadBeregningsperiode>,
): BeregningsperioderFlereAarOpphoer {
    val sisteBeregningsperiode =
        beregningsperioder
            .filter {
                it.datoFOM.year == beregningsperioder.first().datoFOM.year
            }.maxBy { it.datoFOM }
    val sisteBeregningsperiodeNesteAar =
        beregningsperioder
            .filter {
                it.datoFOM.year == beregningsperioder.first().datoFOM.year + 1
            }.maxByOrNull { it.datoFOM }

    // Hvis antall innvilga måneder er overstyrt under beregning skal "forventa" opphørsdato vises selv uten opphørFom
    val forventaOpphoersDato =
        when (val opphoer = behandling.opphoerFraOgMed) {
            null -> {
                if (sisteBeregningsperiode.erOverstyrtInnvilgaMaaneder) {
                    val foersteFom = beregningsperioder.first().datoFOM
                    foersteFom.plusMonths(sisteBeregningsperiode.innvilgaMaaneder.toLong())
                } else if (sisteBeregningsperiodeNesteAar != null && sisteBeregningsperiodeNesteAar.erOverstyrtInnvilgaMaaneder) {
                    LocalDate
                        .of(sisteBeregningsperiodeNesteAar.datoFOM.year, 1, 1)
                        .plusMonths(sisteBeregningsperiodeNesteAar.innvilgaMaaneder.toLong())
                } else {
                    null
                }
            }

            else -> {
                opphoer.atDay(1)
            }
        }
    return BeregningsperioderFlereAarOpphoer(
        sisteBeregningsperiode = sisteBeregningsperiode,
        sisteBeregningsperiodeNesteAar = sisteBeregningsperiodeNesteAar,
        forventetOpphoerDato = forventaOpphoersDato,
    )
}
