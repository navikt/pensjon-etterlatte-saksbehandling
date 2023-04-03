package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDateTime
import java.util.*

data class BehandlingMedGrunnlagsopplysninger<Person>(
    val id: UUID,
    val soeknadMottattDato: LocalDateTime?,
    val personopplysning: Grunnlagsopplysning<Person>?
)