package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.tidspunkt.Tidspunkt
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDto(
    val id: UUID,
    val sak: Long,
    val sakType: SakType,
    val gyldighetsprøving: GyldighetsResultat?,
    val saksbehandlerId: String?,
    val datoFattet: Tidspunkt?,
    val datoAttestert: Tidspunkt?,
    val attestant: String?,
    val soeknadMottattDato: LocalDateTime?,
    val virkningstidspunkt: Virkningstidspunkt?,
    val status: BehandlingStatus,
    val hendelser: List<LagretHendelse>?,
    val familieforhold: Familieforhold?,
    val behandlingType: BehandlingType,
    val søker: Person?,
    val kommerBarnetTilgode: KommerBarnetTilgode?
)

data class Familieforhold(
    val avdoede: Grunnlagsopplysning<Person>?,
    val gjenlevende: Grunnlagsopplysning<Person>?
)