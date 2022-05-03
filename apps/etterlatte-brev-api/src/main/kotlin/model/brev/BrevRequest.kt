package no.nav.etterlatte.model.brev

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak

abstract class BrevRequest {
    abstract val spraak: Spraak
    abstract fun templateName(): String
}
