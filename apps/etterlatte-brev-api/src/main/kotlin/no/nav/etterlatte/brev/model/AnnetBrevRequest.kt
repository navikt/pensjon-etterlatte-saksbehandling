package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.Mal

class AnnetBrevRequest(
    private val mal: Mal,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: MottakerRequest,
    override val attestant: Attestant
) : BrevRequest() {
    override fun templateName(): String = mal.navn
}