package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.libs.common.Vedtaksloesning

data class OmregnetBPNyttRegelverk(
    val utbetaltFoerReform: Int,
    val utbetaltEtterReform: Int,
    val anvendtTrygdetid: Int,
    val grunnbeloep: Int,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            migreringRequest: MigreringBrevRequest?,
        ): OmregnetBPNyttRegelverk {
            val utbetaltFoerReform =
                if (generellBrevData.systemkilde == Vedtaksloesning.PESYS) {
                    requireNotNull(migreringRequest) {
                        "Kan ikke generere brev for migrering fra pesys hvis vi ikke har migreringsdata"
                    }.beregning.brutto
                } else {
                    0 // TODO skal komme fra utbetalingen i siste vedtak i gjenny
                }

            return OmregnetBPNyttRegelverk(
                utbetaltFoerReform = utbetaltFoerReform,
                utbetaltEtterReform = utbetalingsinfo.beloep.value,
                anvendtTrygdetid = utbetalingsinfo.beregningsperioder.first().trygdetid,
                grunnbeloep = utbetalingsinfo.beregningsperioder.first().grunnbeloep.value,
            )
        }
    }
}
