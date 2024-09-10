package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.Vedtaksloesning
import no.nav.etterlatte.libs.common.sak.SakMedUtlandstilknytning
import java.time.LocalDateTime
import java.util.UUID

data class SakMedBehandlinger(
    val sak: SakMedUtlandstilknytning,
    val behandlinger: List<BehandlingSammendrag>,
)

data class BehandlingSammendrag(
    val id: UUID,
    val sak: Long,
    val sakType: SakType,
    val status: BehandlingStatus,
    val soeknadMottattDato: LocalDateTime?,
    val behandlingOpprettet: LocalDateTime?,
    val behandlingType: BehandlingType,
    val aarsak: String?,
    val virkningstidspunkt: Virkningstidspunkt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val kilde: Vedtaksloesning,
)
