package no.nav.etterlatte.brev.model

import no.nav.etterlatte.brev.Mal
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak

class AnnetBrevRequest(
    private val mal: Mal,
    override val spraak: Spraak,
    override val avsender: Avsender,
    override val mottaker: MottakerRequest,
    override val attestant: Attestant
) : BrevRequest() {
    override fun templateName(): String = mal.navn
}