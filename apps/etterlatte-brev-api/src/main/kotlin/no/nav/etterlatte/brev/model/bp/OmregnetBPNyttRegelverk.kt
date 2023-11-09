package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.libs.common.Vedtaksloesning

data class OmregnetBPNyttRegelverk(
    val utbetaltFoerReform: Int,
    val utbetaltEtterReform: Int,
    val anvendtTrygdetid: Int,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
        ): OmregnetBPNyttRegelverk {
            val utbetaltFoerReform =
                if (generellBrevData.systemkilde == Vedtaksloesning.PESYS) {
                    0 // TODO skal komme fra migrering
                } else {
                    0 // TODO skal komme fra utbetalingen i siste vedtak i gjenny
                }

            return OmregnetBPNyttRegelverk(
                utbetaltFoerReform = utbetaltFoerReform,
                utbetaltEtterReform = utbetalingsinfo.beloep.value,
                anvendtTrygdetid = utbetalingsinfo.beregningsperioder.first().trygdetid,
            )
        }
    }
}
