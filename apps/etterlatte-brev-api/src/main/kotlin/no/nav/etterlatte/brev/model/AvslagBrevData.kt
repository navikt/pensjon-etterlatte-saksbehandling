package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Soeker

data class AvslagBrevData(
    val saksnummer: String,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val avsender: Avsender,
    override val mottaker: BrevMottaker,
    override val attestant: Attestant?
) : BrevData() {
    override fun templateName(): String = "avslag"

    companion object {
        fun fraVedtak(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: BrevMottaker,
            attestant: Attestant?
        ): AvslagBrevData =
            AvslagBrevData(
                saksnummer = behandling.sakId.toString(),
                avdoed = behandling.persongalleri.avdoed,
                aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår?
                avsender = avsender,
                mottaker = mottaker,
                attestant = attestant
            )
    }
}