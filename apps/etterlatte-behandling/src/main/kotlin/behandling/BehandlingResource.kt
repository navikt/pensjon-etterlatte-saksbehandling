package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import java.time.LocalDateTime
import java.util.*

data class BehandlingListe(val behandlinger: List<BehandlingSammendrag>)

data class BehandlingSammendrag(
    val id: UUID,
    val sak: Long,
    val status: BehandlingStatus,
    val soeknadMottattDato: LocalDateTime?,
    val behandlingOpprettet: LocalDateTime?,
    val behandlingType: BehandlingType,
    val aarsak: String?,
    val virkningstidspunkt: Virkningstidspunkt?
)