package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling
import no.nav.etterlatte.brev.behandling.Soeker
import no.nav.etterlatte.libs.common.brev.model.Spraak

data class AvslagBrevRequest(
    val saksnummer: String,
    val barn: Soeker,
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: MottakerRequest,
    override val attestant: Attestant?
) : BrevRequest() {
    override fun templateName(): String = "avslag"

    companion object {
        fun fraVedtak(
            behandling: Behandling,
            avsender: Avsender,
            mottaker: MottakerRequest,
            attestant: Attestant?
        ): AvslagBrevRequest =
            AvslagBrevRequest(
                saksnummer = behandling.sakId.toString(),
                barn = behandling.persongalleri.soeker,
                avdoed = behandling.persongalleri.avdoed,
                aktuelleParagrafer = emptyList(), // todo: Gå igjennom oppfylte vilkår?
                spraak = behandling.spraak,
                avsender = avsender,
                mottaker = mottaker,
                attestant = attestant
            )
    }
}