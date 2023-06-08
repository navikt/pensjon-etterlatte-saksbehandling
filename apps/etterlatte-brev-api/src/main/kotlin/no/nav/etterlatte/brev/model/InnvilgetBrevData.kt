package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo

data class InnvilgetBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val avdoed: Avdoed
) : BrevData() {

    companion object {
        fun fra(behandling: Behandling): InnvilgetBrevData =
            InnvilgetBrevData(
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                avdoed = behandling.persongalleri.avdoed
            )
    }
}