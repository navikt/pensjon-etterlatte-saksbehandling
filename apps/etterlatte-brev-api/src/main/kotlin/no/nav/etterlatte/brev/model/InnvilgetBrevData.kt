package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo

data class InnvilgetBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avdoed: Avdoed,
    override val avsender: Avsender,
    override val mottaker: BrevMottaker,
    override val attestant: Attestant?
) : BrevData() {
    override fun templateName(): String = "innvilget"

    companion object {
        fun fraVedtak(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: BrevMottaker,
            attestant: Attestant?
        ): InnvilgetBrevData =
            InnvilgetBrevData(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                avdoed = behandling.persongalleri.avdoed,
                mottaker = mottaker,
                avsender = avsender,
                attestant = attestant
            )
    }
}