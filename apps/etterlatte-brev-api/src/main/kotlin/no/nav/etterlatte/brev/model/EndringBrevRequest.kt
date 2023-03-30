package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak

data class EndringBrevRequest(
    val saksnummer: String,
    val utbetalingsinfo: Utbetalingsinfo,
    val barn: Soeker,
    val avdoed: Avdoed,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: MottakerRequest,
    override val attestant: Attestant?
) : BrevRequest() {
    override fun templateName(): String = "innvilget"

    companion object {
        fun fraVedtak(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: MottakerRequest,
            attestant: Attestant?
        ): EndringBrevRequest =
            EndringBrevRequest(
                saksnummer = behandling.sakId.toString(),
                utbetalingsinfo = behandling.utbetalingsinfo!!,
                barn = behandling.persongalleri.soeker,
                avdoed = behandling.persongalleri.avdoed,
                spraak = behandling.spraak,
                mottaker = mottaker,
                avsender = avsender,
                attestant = attestant
            )
    }
}