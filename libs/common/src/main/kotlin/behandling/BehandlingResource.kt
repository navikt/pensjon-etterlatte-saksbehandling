package no.nav.etterlatte.libs.common.behandling

import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
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

data class DetaljertBehandling(
    val id: UUID,
    val sak: Long,
    val sakType: SakType,
    val behandlingOpprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val soeknadMottattDato: LocalDateTime?,
    val innsender: String?,
    val soeker: String,
    val gjenlevende: List<String>?,
    val avdoed: List<String>?,
    val soesken: List<String>?,
    val gyldighetsproeving: GyldighetsResultat?,
    val status: BehandlingStatus,
    val behandlingType: BehandlingType,
    val virkningstidspunkt: Virkningstidspunkt?,
    val kommerBarnetTilgode: KommerBarnetTilgode?,
    val revurderingsaarsak: RevurderingAarsak? = null
)

data class BehandlingMedGrunnlagsopplysninger<Person>(
    val id: UUID,
    val soeknadMottattDato: LocalDateTime?,
    val personopplysning: Grunnlagsopplysning<Person>?
)