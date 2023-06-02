package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling

data class AvslagBrevData(
    val saksnummer: String,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val avsender: Avsender,
    override val mottaker: BrevMottaker,
) : BrevData() {
    override fun templateName(): String = "avslag"

    companion object {
        fun fraVedtak(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: BrevMottaker,
        ): AvslagBrevData =
            AvslagBrevData(
                saksnummer = behandling.sakId.toString(),
                avdoed = behandling.persongalleri.avdoed,
                aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår?
                avsender = avsender,
                mottaker = mottaker,
            )
    }
}