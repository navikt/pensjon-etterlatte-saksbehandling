package no.nav.etterlatte.brev.model.bp

import no.nav.etterlatte.brev.MigreringBrevRequest
import no.nav.etterlatte.brev.behandling.GenerellBrevData
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.brev.model.BrevData
import no.nav.etterlatte.libs.common.IntBroek
import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.pensjon.brevbaker.api.model.Kroner

data class OmregnetBPNyttRegelverk(
    val utbetaltFoerReform: Kroner,
    val utbetaltEtterReform: Kroner,
    val anvendtTrygdetid: Int,
    val prorataBroek: IntBroek?,
    val grunnbeloep: Kroner,
) : BrevData() {
    companion object {
        fun fra(
            generellBrevData: GenerellBrevData,
            utbetalingsinfo: Utbetalingsinfo,
            migreringRequest: MigreringBrevRequest?,
        ): OmregnetBPNyttRegelverk {
            val foersteBeregningsperiode = utbetalingsinfo.beregningsperioder.first()
            val utbetaltFoerReform =
                if (generellBrevData.systemkilde == Vedtaksloesning.PESYS) {
                    requireNotNull(migreringRequest) {
                        "Kan ikke generere brev for migrering fra pesys hvis vi ikke har migreringsdata"
                    }.brutto
                } else {
                    0 // TODO skal komme fra utbetalingen i siste vedtak i gjenny
                }

            return OmregnetBPNyttRegelverk(
                utbetaltFoerReform = Kroner(utbetaltFoerReform),
                utbetaltEtterReform = Kroner(utbetalingsinfo.beloep.value),
                anvendtTrygdetid = foersteBeregningsperiode.trygdetid,
                prorataBroek = foersteBeregningsperiode.prorataBroek,
                grunnbeloep = Kroner(foersteBeregningsperiode.grunnbeloep.value),
            )
        }
    }
}
