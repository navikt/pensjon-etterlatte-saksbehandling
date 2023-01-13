package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.beregning.BeregningDTO
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vilkaarsvurdering.VilkaarsvurderingDto
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDto(
    val id: UUID,
    val sak: Long,
    val gyldighetsprøving: GyldighetsResultat?,
    val vilkårsprøving: VilkaarsvurderingDto?,
    val beregning: BeregningDTO?,
    val saksbehandlerId: String?,
    val fastsatt: Boolean?,
    val datoFattet: Instant?,
    val datoattestert: Instant?,
    val attestant: String?,
    val soeknadMottattDato: LocalDateTime?,
    val virkningstidspunkt: Virkningstidspunkt?,
    val status: BehandlingStatus?,
    val hendelser: List<LagretHendelse>?,
    val familieforhold: Familieforhold?,
    val behandlingType: BehandlingType?,
    val søker: Person?,
    val kommerBarnetTilgode: KommerBarnetTilgode?
)

data class Familieforhold(
    val avdoede: Grunnlagsopplysning<Person>?,
    val gjenlevende: Grunnlagsopplysning<Person>?
)