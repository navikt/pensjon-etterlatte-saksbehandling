package no.nav.etterlatte.model.brev

import no.nav.etterlatte.libs.common.soeknad.dataklasser.common.Spraak
import java.time.LocalDate

abstract class BrevRequest {
    abstract val spraak: Spraak
    abstract val mottaker: Mottaker
    val utsendingsDato = LocalDate.now()
    abstract fun templateName(): String
}

data class Mottaker(val navn: String, val adresse: String, val postnummer: String)
