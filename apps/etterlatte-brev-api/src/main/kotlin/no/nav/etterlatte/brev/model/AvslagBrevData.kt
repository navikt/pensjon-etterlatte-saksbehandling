package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.behandling.Behandling

object AvslagBrevData : BrevData() {
    fun fra(behandling: Behandling): AvslagBrevData = AvslagBrevData
}