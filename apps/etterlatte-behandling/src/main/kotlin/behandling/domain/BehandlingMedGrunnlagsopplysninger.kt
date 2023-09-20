package no.nav.etterlatte.behandling.domain

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingMedGrunnlagsopplysninger<Person>(
    val id: UUID,
    val soeknadMottattDato: LocalDateTime?,
    val personopplysning: Grunnlagsopplysning<Person>?,
)
