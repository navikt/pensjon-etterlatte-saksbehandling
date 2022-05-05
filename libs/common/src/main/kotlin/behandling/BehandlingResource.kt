package no.nav.etterlatte.libs.common.behandling

import java.time.LocalDateTime
import java.util.*

data class BehandlingSammendragListe(val behandlinger: List<BehandlingSammendrag>)

data class BehandlingSammendrag(val id: UUID, val sak: Long, val status: BehandlingStatus?, val mottattDato: LocalDateTime?)

data class DetaljertBehandling(
        val id: UUID,
        val sak: Long,
//TODO: hva mer skal vi ha med her?
)