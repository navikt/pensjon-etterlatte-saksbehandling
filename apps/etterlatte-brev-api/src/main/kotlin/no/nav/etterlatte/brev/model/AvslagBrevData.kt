package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling

object AvslagBrevData : BrevData() {
    // TODO: denne skal ikke ha hele behandlingen inn
    fun fra(behandling: Behandling): AvslagBrevData = AvslagBrevData
}
