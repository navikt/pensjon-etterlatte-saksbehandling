package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Avdoed
import no.nav.etterlatte.brev.behandling.Behandling

data class AvslagBrevData(
    val avdoed: Avdoed,
    val aktuelleParagrafer: List<String>
) : BrevData() {
    companion object {
        fun fra(behandling: Behandling): AvslagBrevData =
            AvslagBrevData(
                avdoed = behandling.persongalleri.avdoed,
                aktuelleParagrafer = emptyList() // todo: Gå igjennom oppfylte vilkår?
            )
    }
}