package no.nav.etterlatte.behandling

import no.nav.etterlatte.behandling.hendelse.LagretHendelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.BehandlingType
import no.nav.etterlatte.libs.common.behandling.BoddEllerArbeidetUtlandet
import no.nav.etterlatte.libs.common.behandling.KommerBarnetTilgode
import no.nav.etterlatte.libs.common.behandling.RevurderingAarsak
import no.nav.etterlatte.libs.common.behandling.RevurderingInfo
import no.nav.etterlatte.libs.common.behandling.SakType
import no.nav.etterlatte.libs.common.behandling.Utenlandstilsnitt
import no.nav.etterlatte.libs.common.behandling.Virkningstidspunkt
import no.nav.etterlatte.libs.common.grunnlag.Grunnlagsopplysning
import no.nav.etterlatte.libs.common.gyldigSoeknad.GyldighetsResultat
import no.nav.etterlatte.libs.common.person.Person
import java.time.LocalDateTime
import java.util.*

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
    val utenlandstilsnitt: Utenlandstilsnitt?,
    val boddEllerArbeidetUtlandet: BoddEllerArbeidetUtlandet?,
    val status: BehandlingStatus,
    val hendelser: List<LagretHendelse>?,
    val familieforhold: Familieforhold?,
    val behandlingType: BehandlingType,
    val søker: Person?,
    val kommerBarnetTilgode: KommerBarnetTilgode?,
    val revurderingsaarsak: RevurderingAarsak?,
    val revurderinginfo: RevurderingInfo?,
    val begrunnelse: String?
)

data class Familieforhold(
    val avdoede: Grunnlagsopplysning<Person>?,
    val gjenlevende: Grunnlagsopplysning<Person>?
)