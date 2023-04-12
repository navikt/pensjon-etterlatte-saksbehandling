package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.brev.behandling.Utbetalingsinfo

data class InnvilgetBrevRequest(
    val saksnummer: String,
    val utbetalingsinfo: Utbetalingsinfo,
    val barn: Soeker,
    val avdoed: Avdoed,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: BrevMottaker,
    override val attestant: Attestant?
) : BrevRequest() {
    override fun templateName(): String = "innvilget"

    companion object {
        fun fraVedtak(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: BrevMottaker,
            attestant: Attestant?
        ): InnvilgetBrevRequest =
            InnvilgetBrevRequest(
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