package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak

class EndringBrevRequest(
    val saksnummer: String,
    val utbetalingsinfo: Utbetalingsinfo,
    val barn: Soeker,
    val revurderingsaarsak: RevurderingAarsak,
    val avdoed: Avdoed,
    override val avsender: Avsender,
    override val mottaker: BrevMottaker,
    override val attestant: Attestant?
) : BrevData() {

    override fun templateName(): String = "endring"

    companion object {
        fun fraVedtak(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: BrevMottaker,
            attestant: Attestant?
        ): EndringBrevRequest = EndringBrevRequest(
            avsender = avsender,
            mottaker = mottaker,
            attestant = attestant,
            saksnummer = behandling.sakId.toString(),
            utbetalingsinfo = behandling.utbetalingsinfo!!,
            barn = behandling.persongalleri.soeker,
            avdoed = behandling.persongalleri.avdoed,
            revurderingsaarsak = requireNotNull(behandling.revurderingsaarsak) {
                "Endringsbrev trenger en revurderingsaarsak"
            }
        )
    }
}