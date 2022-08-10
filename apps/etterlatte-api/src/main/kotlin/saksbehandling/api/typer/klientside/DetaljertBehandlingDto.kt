package no.nav.etterlatte.saksbehandling.api.typer.klientside

import no.nav.etterlatte.libs.common.avkorting.AvkortingsResultat
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.beregning.BeregningsResultat
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.person.Person
import no.nav.etterlatte.libs.common.vikaar.KommerSoekerTilgode
import no.nav.etterlatte.libs.common.vikaar.VilkaarResultat
import no.nav.etterlatte.typer.LagretHendelse
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDto(
    val id: UUID,
    val sak: Long,
    val gyldighetsprøving: GyldighetsResultat?,
    val kommerSoekerTilgode: KommerSoekerTilgode?,
    val vilkårsprøving: VilkaarResultat?,
    val beregning: BeregningsResultat?,
    val avkortning: AvkortingsResultat?,
    val saksbehandlerId: String?,
    val fastsatt: Boolean?,
    val datoFattet: Instant?,
    val datoattestert: Instant?,
    val attestant: String?,
    val soeknadMottattDato: LocalDateTime?,
    val virkningstidspunkt: LocalDate?,
    val status: BehandlingStatus?,
    val hendelser: List<LagretHendelse>?,
    val familieforhold: Familieforhold?,
)

data class Familieforhold(
    val avdoede: Grunnlagsopplysning<Person>?,
    val gjenlevende: Grunnlagsopplysning<Person>?
)
