package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling

data class ManueltBrevRequest(
    val saksnummer: String,
    val innhold: Slate,
    val avdoed: Avdoed,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: BrevMottaker,
    override val attestant: Attestant?
) : BrevRequest() {
    override fun templateName(): String = "manuell"

    companion object {
        fun fraVedtak(
            innhold: Slate,
            behandling: Behandling,
            avsender: Avsender,
            mottaker: Mottaker,
            attestant: Attestant?
        ): ManueltBrevRequest =
            ManueltBrevRequest(
                saksnummer = behandling.sakId.toString(),
                innhold = innhold,
                avdoed = behandling.persongalleri.avdoed,
                spraak = behandling.spraak,
                mottaker = BrevMottaker.fra(mottaker),
                avsender = avsender,
                attestant = attestant
            )
    }
}