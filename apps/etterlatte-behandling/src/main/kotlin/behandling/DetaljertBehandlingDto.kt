package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.behandling.revurdering.RevurderingInfoMedBegrunnelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.Etterbetaling
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utenlandstilknytning
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.person.Person
import java.time.LocalDateTime
import java.util.UUID

/**
 * Brukes av frontend. Se IDetaljertBehandling.ts
 **/
data class DetaljertBehandlingDto(
    val id: UUID,
    val sakId: Long,
    val sakType: SakType,
    val gyldighetsprøving: GyldighetsResultat?,
    val soeknadMottattDato: LocalDateTime?,
    val virkningstidspunkt: Virkningstidspunkt?,
    val utenlandstilknytning: Utenlandstilknytning?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val status: BehandlingStatus,
    val hendelser: List<LagretHendelse>?,
    val familieforhold: Familieforhold?,
    val behandlingType: BehandlingType,
    val søker: Person?,
    val kommerBarnetTilgode: KommerBarnetTilgode?,
    val revurderingsaarsak: Revurderingaarsak?,
    val revurderinginfo: RevurderingInfoMedBegrunnelse?,
    val begrunnelse: String?,
    val etterbetaling: Etterbetaling?,
)

data class Familieforhold(
    val avdoede: Grunnlagsopplysning<Person>?,
)
