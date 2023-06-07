package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak

class EndringBrevData(
    val utbetalingsinfo: Utbetalingsinfo,
    val barn: Soeker,
    val revurderingsaarsak: RevurderingAarsak,
    val avdoed: Avdoed
) : BrevData() {
    companion object {
        fun fra(behandling: Behandling): EndringBrevData = EndringBrevData(
            utbetalingsinfo = behandling.utbetalingsinfo!!,
            barn = behandling.persongalleri.soeker,
            avdoed = behandling.persongalleri.avdoed,
            revurderingsaarsak = requireNotNull(behandling.revurderingsaarsak) {
                "Endringsbrev trenger en revurderingsaarsak"
            }
        )
    }
}