package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import java.time.LocalDateTime
import java.util.*

data class Behandling(
    val id: UUID,
    val sak: Long,
    val behandlingOpprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val soeknadMottattDato: LocalDateTime?,
    val innsender: String?,
    val soeker: String?,
    val gjenlevende: List<String>?,
    val avdoed: List<String>?,
    val soesken: List<String>?,
    val gyldighetsproeving: GyldighetsResultat?,
    val status: BehandlingStatus?,
)
