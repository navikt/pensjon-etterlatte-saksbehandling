package model.brev

import no.nav.etterlatte.Mal
import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import no.nav.etterlatte.model.brev.BrevRequest
import no.nav.etterlatte.model.brev.Mottaker

class AnnetBrevRequest(
    val mal: Mal,
    override val spraak: Spraak,
    override val mottaker: Mottaker
) : BrevRequest() {
    override fun templateName(): String = mal.navn
}