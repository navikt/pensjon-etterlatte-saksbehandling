package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingUtfall
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
    val virkningstidspunkt: Virkningstidspunkt?,
    val vilkaarsvurderingUtfall: VilkaarsvurderingUtfall?
)