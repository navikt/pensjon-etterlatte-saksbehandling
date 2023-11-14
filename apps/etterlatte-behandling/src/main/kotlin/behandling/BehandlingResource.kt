package no.nav.etterlatte.behandling

import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.sak.Sak
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingListe(
    val sak: Sak,
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
)
